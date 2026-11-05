import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EMPTY, Observable, of, throwError } from 'rxjs';
import { AgendaApi } from '../../core/api/agenda.api';
import { AuthService } from '../../core/auth/auth.service';
import { API_ERROR_CODES } from '../../core/http/api-error';
import { Availability, ScheduleException } from '../../core/models/api.models';
import { toIso } from '../../core/utils/calendar.util';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { AvailabilityService } from './availability.service';

const TODAY = toIso(new Date());

const PARTIAL_REQUEST = {
  date: '2026-11-12',
  isFullDayBlock: false,
  startTime: '09:00',
  endTime: '10:00',
  reason: null,
};

function block(overrides: Partial<Availability> = {}): Availability {
  return {
    id: 1,
    dayOfWeek: 'MONDAY',
    startTime: '08:00:00',
    endTime: '12:00:00',
    slotDurationMinutes: 30,
    active: true,
    ...overrides,
  };
}

function exception(overrides: Partial<ScheduleException> = {}): ScheduleException {
  return {
    id: 3,
    date: '2026-11-12',
    startTime: null,
    endTime: null,
    isFullDayBlock: true,
    reason: null,
    ...overrides,
  };
}

function conflictError(count?: unknown): HttpErrorResponse {
  return new HttpErrorResponse({
    status: 409,
    error: {
      status: 409,
      message: 'Hay citas activas',
      code: API_ERROR_CODES.activeAppointmentsConflict,
      details: count === undefined ? undefined : { count },
    },
  });
}

describe('AvailabilityService', () => {
  let service: AvailabilityService;
  let api: Record<
    | 'availabilities'
    | 'exceptions'
    | 'availableSlots'
    | 'addAvailability'
    | 'removeAvailability'
    | 'addException'
    | 'removeException',
    ReturnType<typeof vi.fn>
  >;
  let confirm: { ask: ReturnType<typeof vi.fn>; confirmThen: ReturnType<typeof vi.fn> };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function run<T>(source: Observable<T>): { values: T[]; error: unknown } {
    const outcome: { values: T[]; error: unknown } = { values: [], error: undefined };
    source.subscribe({ next: (v) => outcome.values.push(v), error: (e) => (outcome.error = e) });
    return outcome;
  }

  beforeEach(async () => {
    api = {
      availabilities: vi.fn(() =>
        of([
          block({ id: 2, startTime: '14:00:00', endTime: '18:00:00' }),
          block({ id: 1 }),
          block({ id: 3, dayOfWeek: 'FRIDAY' }),
        ]),
      ),
      exceptions: vi.fn(() => of([exception()])),
      availableSlots: vi.fn(() =>
        of({
          date: TODAY,
          doctorId: 10,
          availableSlots: [{ startTime: '08:00:00', endTime: '08:30:00' }],
        }),
      ),
      addAvailability: vi.fn(() => of(block())),
      removeAvailability: vi.fn(() => of(undefined)),
      addException: vi.fn(() => of(exception())),
      removeException: vi.fn(() => of(undefined)),
    };
    confirm = { ask: vi.fn(() => of(true)), confirmThen: vi.fn(() => EMPTY) };
    notifications = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        AvailabilityService,
        { provide: AgendaApi, useValue: api },
        { provide: AuthService, useValue: { userId: signal(10) } },
        { provide: ConfirmService, useValue: confirm },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    service = TestBed.inject(AvailabilityService);
    await settle();
  });

  describe('loading', () => {
    it('groups weekly blocks by day sorted by start time', () => {
      const week = service.week();
      expect(week).toHaveLength(7);
      expect(week[0].value).toBe('MONDAY');
      expect(week[0].blocks.map((b) => b.id)).toEqual([1, 2]);
      expect(week[4].blocks.map((b) => b.id)).toEqual([3]);
      expect(week[1].blocks).toEqual([]);
    });

    it('loads exceptions from today and the preview slots of the doctor', () => {
      expect(api.exceptions).toHaveBeenCalledWith(TODAY);
      expect(api.availableSlots).toHaveBeenCalledWith(10, TODAY);
      expect(service.exceptions()).toEqual([exception()]);
      expect(service.previewSlots()).toEqual(['08:00']);
    });

    it('reloads the preview for a new date and ignores empty dates', async () => {
      service.setPreviewDate('2026-11-20');
      service.setPreviewDate('');
      await settle();
      expect(service.previewDate()).toBe('2026-11-20');
      expect(api.availableSlots).toHaveBeenLastCalledWith(10, '2026-11-20');
    });
  });

  describe('addAvailability', () => {
    const value = { startTime: '08:00', endTime: '12:00', slotDurationMinutes: 30 };

    it('saves each day in sequence, notifies and reloads', async () => {
      api.availabilities.mockClear();
      const outcome = run(service.addAvailability(['MONDAY', 'TUESDAY'], value));
      await settle();

      expect(api.addAvailability.mock.calls.map((c) => c[0].dayOfWeek)).toEqual([
        'MONDAY',
        'TUESDAY',
      ]);
      expect(api.addAvailability).toHaveBeenCalledWith({ dayOfWeek: 'MONDAY', ...value });
      expect(outcome.values).toEqual([{ saved: ['MONDAY', 'TUESDAY'], failed: [] }]);
      expect(notifications.success).toHaveBeenCalledWith('Horario agregado');
      expect(api.availabilities).toHaveBeenCalledOnce();
    });

    it('reports failed days with their messages without notifying success', () => {
      api.addAvailability.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Solapado' } })),
      );
      const outcome = run(service.addAvailability(['MONDAY'], value));

      expect(outcome.values).toEqual([
        { saved: [], failed: [{ day: 'MONDAY', message: 'Solapado' }] },
      ]);
      expect(notifications.success).not.toHaveBeenCalled();
    });
  });

  describe('addException', () => {
    it('saves without cancelling appointments and notifies', async () => {
      api.exceptions.mockClear();
      const outcome = run(service.addException(PARTIAL_REQUEST));
      await settle();

      expect(api.addException).toHaveBeenCalledWith({
        ...PARTIAL_REQUEST,
        cancelAppointments: false,
      });
      expect(outcome.values).toEqual([undefined]);
      expect(notifications.success).toHaveBeenCalledWith('Agenda bloqueada');
      expect(api.exceptions).toHaveBeenCalledOnce();
    });

    it('propagates errors that are not an appointments conflict', () => {
      const error = new HttpErrorResponse({ status: 400, error: { message: 'Fecha inválida' } });
      api.addException.mockReturnValue(throwError(() => error));

      const outcome = run(service.addException(PARTIAL_REQUEST));

      expect(outcome.error).toBe(error);
      expect(confirm.ask).not.toHaveBeenCalled();
      expect(notifications.success).not.toHaveBeenCalled();
    });

    it('propagates a 409 without the conflict code instead of asking', () => {
      const error = new HttpErrorResponse({ status: 409, error: { message: 'Hay 2 citas' } });
      api.addException.mockReturnValue(throwError(() => error));

      const outcome = run(service.addException(PARTIAL_REQUEST));

      expect(outcome.error).toBe(error);
      expect(confirm.ask).not.toHaveBeenCalled();
    });

    it.each([
      [
        1,
        'Hay 1 cita activa en este horario. Si bloqueas, se cancelará y se notificará al paciente.',
      ],
      [
        3,
        'Hay 3 citas activas en este horario. Si bloqueas, se cancelarán y se notificará a los pacientes.',
      ],
      [
        undefined,
        'Hay citas activas en este horario. Si bloqueas, se cancelarán y se notificará a los pacientes.',
      ],
    ])('asks before cancelling %s active appointments', (count, message) => {
      api.addException.mockReturnValueOnce(throwError(() => conflictError(count)));

      run(service.addException(PARTIAL_REQUEST));

      expect(confirm.ask).toHaveBeenCalledWith({
        title: 'Cancelar citas',
        message,
        confirmLabel: 'Bloquear y cancelar citas',
        danger: true,
      });
    });

    it('retries cancelling appointments when confirmed', () => {
      api.addException.mockReturnValueOnce(throwError(() => conflictError(1)));

      const outcome = run(service.addException(PARTIAL_REQUEST));

      expect(api.addException).toHaveBeenLastCalledWith({
        ...PARTIAL_REQUEST,
        cancelAppointments: true,
      });
      expect(outcome.values).toEqual([undefined]);
      expect(notifications.success).toHaveBeenCalledWith(
        'Agenda bloqueada. Se canceló la cita y se notificó al paciente',
      );
    });

    it('uses the plural success message for several appointments', () => {
      api.addException.mockReturnValueOnce(throwError(() => conflictError(2)));
      run(service.addException(PARTIAL_REQUEST));
      expect(notifications.success).toHaveBeenCalledWith(
        'Agenda bloqueada. Se cancelaron las citas y se notificó a los pacientes',
      );
    });

    it('completes silently when the user declines', () => {
      api.addException.mockReturnValueOnce(throwError(() => conflictError(2)));
      confirm.ask.mockReturnValue(of(false));

      const outcome = run(service.addException(PARTIAL_REQUEST));

      expect(api.addException).toHaveBeenCalledOnce();
      expect(outcome.values).toEqual([]);
      expect(outcome.error).toBeUndefined();
      expect(notifications.success).not.toHaveBeenCalled();
    });
  });

  describe('remove commands', () => {
    it('confirms before removing a weekly block and reloads after it', async () => {
      confirm.confirmThen.mockImplementation((_options, action: () => Observable<unknown>) =>
        action(),
      );
      api.availabilities.mockClear();

      run(service.removeAvailability(block({ id: 5 }), 'Lunes'));
      await settle();

      const [options, , success, fallback] = confirm.confirmThen.mock.calls[0];
      expect(options).toMatchObject({
        title: 'Quitar horario',
        message:
          '¿Quitar el bloque del lunes de 08:00 a 12:00? Las citas ya agendadas se mantienen.',
        confirmLabel: 'Quitar',
        danger: true,
      });
      expect(success).toBe('Horario quitado');
      expect(fallback).toBe('No se pudo quitar el horario');
      expect(api.removeAvailability).toHaveBeenCalledWith(5);
      expect(api.availabilities).toHaveBeenCalledOnce();
    });

    it('confirms before removing an exception', () => {
      confirm.confirmThen.mockImplementation((_options, action: () => Observable<unknown>) =>
        action(),
      );

      run(service.removeException(exception({ id: 8, date: '2026-11-12' })));

      expect(confirm.confirmThen.mock.calls[0][0]).toMatchObject({
        title: 'Quitar bloqueo',
        message: '¿Habilitar nuevamente el jueves, 12 de noviembre de 2026?',
      });
      expect(api.removeException).toHaveBeenCalledWith(8);
    });

    it('does not reload when the removal is cancelled', async () => {
      api.availabilities.mockClear();
      run(service.removeAvailability(block(), 'Lunes'));
      await settle();
      expect(api.availabilities).not.toHaveBeenCalled();
    });
  });
});
