import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EMPTY, Observable, of, throwError } from 'rxjs';
import { PatientsApi } from '../../core/api/patients.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { Page, Patient } from '../../core/models/api.models';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { PatientsService } from './patients.service';

function patient(overrides: Partial<Patient> = {}): Patient {
  return {
    id: 20,
    ci: '1234567',
    name: 'Ana Pérez',
    email: null,
    phoneNumber: '70000000',
    active: true,
    ...overrides,
  };
}

function page(content: Patient[], totalElements = content.length, totalPages = 1): Page<Patient> {
  return { content, page: { size: 12, number: 0, totalElements, totalPages } };
}

describe('PatientsService', () => {
  let service: PatientsService;
  let patientsApi: Record<'list' | 'deactivate' | 'activate', ReturnType<typeof vi.fn>>;
  let staffApi: Record<'patientsOf' | 'assignPatient' | 'removePatient', ReturnType<typeof vi.fn>>;
  let confirm: { confirmThen: ReturnType<typeof vi.fn> };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  async function setup(): Promise<void> {
    patientsApi = {
      list: vi.fn(() => of(page([patient()], 30, 3))),
      deactivate: vi.fn(() => of(undefined)),
      activate: vi.fn(() => of(patient())),
    };
    staffApi = {
      patientsOf: vi.fn(() => of([])),
      assignPatient: vi.fn(() => of(undefined)),
      removePatient: vi.fn(() => of(undefined)),
    };
    confirm = {
      confirmThen: vi.fn((_options, action: () => Observable<unknown>) => action()),
    };
    notifications = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        PatientsService,
        { provide: PatientsApi, useValue: patientsApi },
        { provide: StaffApi, useValue: staffApi },
        { provide: ConfirmService, useValue: confirm },
        { provide: NotificationService, useValue: notifications },
        {
          provide: AuthService,
          useValue: {
            userId: signal(1),
            hasRole: () => false,
            restrictedToOwnData: signal(false),
          },
        },
      ],
    });
    service = TestBed.inject(PatientsService);
    await settle();
  }

  afterEach(() => vi.useRealTimers());

  describe('listing', () => {
    it('lists active clinic patients on the first page', async () => {
      await setup();
      expect(service.scope()).toBe('all');
      expect(patientsApi.list).toHaveBeenCalledWith({
        search: '',
        isActive: true,
        page: 0,
        size: 12,
        sort: 'id',
      });
      expect(service.patients()).toEqual([patient()]);
      expect(service.rows()).toEqual([{ patient: patient(), mine: false }]);
      expect(service.pagination()).toEqual({ number: 0, totalElements: 30, totalPages: 3 });
    });

    it('hides assignment features while the flag is disabled', async () => {
      await setup();
      expect(service.ownListEnabled).toBe(false);
      expect(service.canManageDoctors).toBe(false);
      expect(staffApi.patientsOf).not.toHaveBeenCalled();
    });

    it('requests the selected page', async () => {
      await setup();
      service.setPage(2);
      await settle();
      expect(patientsApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }));
    });

    it('includes inactive patients and resets the page', async () => {
      await setup();
      service.setPage(2);
      service.setShowInactive(true);
      await settle();
      expect(patientsApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ isActive: undefined, page: 0 }),
      );
    });

    it('debounces the trimmed search and resets the page', async () => {
      vi.useFakeTimers({
        toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'],
      });
      await setup();
      vi.advanceTimersByTime(500);
      service.setPage(2);
      await settle();
      patientsApi.list.mockClear();

      service.setSearch('  ana ');
      TestBed.tick();
      vi.advanceTimersByTime(500);
      await settle();

      expect(patientsApi.list).toHaveBeenCalledOnce();
      expect(patientsApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'ana', page: 0 }),
      );
      expect(service.search()).toBe('  ana ');
    });

    it('exposes the load error message', async () => {
      await setup();
      patientsApi.list.mockReturnValue(throwError(() => new Error('offline')));
      service.reload();
      await settle();
      expect(service.errorMessage()).toBe('No se pudieron cargar los pacientes');
    });
  });

  describe('commands', () => {
    it('deactivates after confirmation, then reloads', async () => {
      await setup();
      patientsApi.list.mockClear();

      service.deactivate(patient()).subscribe();
      await settle();

      const [options, , success, fallback] = confirm.confirmThen.mock.calls[0];
      expect(options).toMatchObject({ title: 'Desactivar paciente', danger: true });
      expect(options.message).toContain('Ana Pérez ya no podrá agendar nuevas citas');
      expect(success).toBe('Paciente desactivado');
      expect(fallback).toBe('No se pudo desactivar el paciente');
      expect(patientsApi.deactivate).toHaveBeenCalledWith(20);
      expect(patientsApi.list).toHaveBeenCalledOnce();
    });

    it('reactivates after confirmation, then reloads', async () => {
      await setup();
      patientsApi.list.mockClear();

      service.activate(patient({ active: false })).subscribe();
      await settle();

      expect(confirm.confirmThen.mock.calls[0][0]).toMatchObject({
        title: 'Reactivar paciente',
        message: '¿Reactivar a Ana Pérez?',
      });
      expect(patientsApi.activate).toHaveBeenCalledWith(20);
      expect(patientsApi.list).toHaveBeenCalledOnce();
    });

    it('does not reload when a confirmation is declined', async () => {
      await setup();
      confirm.confirmThen.mockReturnValue(EMPTY);
      patientsApi.list.mockClear();

      service.deactivate(patient()).subscribe();
      await settle();

      expect(patientsApi.list).not.toHaveBeenCalled();
    });

    it('assigns a patient to the current doctor and notifies', async () => {
      await setup();
      service.toggleMine(patient()).subscribe();
      expect(staffApi.assignPatient).toHaveBeenCalledWith({ patientId: 20, doctorId: 1 });
      expect(notifications.success).toHaveBeenCalledWith('Paciente agregado a tu lista');
    });

    it('notifies the API error when the assignment fails', async () => {
      await setup();
      staffApi.assignPatient.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Ya asignado' } })),
      );
      service.toggleMine(patient()).subscribe();
      expect(notifications.error).toHaveBeenCalledWith('Ya asignado');
    });
  });
});
