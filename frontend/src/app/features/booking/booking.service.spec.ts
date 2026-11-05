import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef, WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { AgendaApi } from '../../core/api/agenda.api';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { PatientsApi } from '../../core/api/patients.api';
import { AuthService } from '../../core/auth/auth.service';
import { DoctorsStore } from '../../core/catalogs/doctors.store';
import { SpecialtiesStore } from '../../core/catalogs/specialties.store';
import { API_ERROR_CODES } from '../../core/http/api-error';
import {
  ApiError,
  AppointmentResponse,
  AvailableSlots,
  Page,
  Patient,
  Specialty,
  Staff,
} from '../../core/models/api.models';
import { addDays, toIso } from '../../core/utils/calendar.util';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { BookingService } from './booking.service';

interface Setup {
  ownDataOnly?: boolean;
  dateParam?: string | null;
}

const TODAY = toIso(new Date());
const TOMORROW = toIso(addDays(new Date(), 1));

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

function doctor(overrides: Partial<Staff> = {}): Staff {
  return {
    id: 10,
    username: 'gomez',
    ci: '555',
    name: 'Dr. Gómez',
    email: 'gomez@clinic.test',
    phoneNumber: null,
    active: true,
    alsoDoctor: false,
    role: 'DOCTOR',
    specialtyId: 1,
    specialtyName: 'Cardiología',
    ...overrides,
  };
}

function page<T>(content: T[]): Page<T> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

function slots(...startTimes: string[]): AvailableSlots {
  return {
    date: TOMORROW,
    doctorId: 10,
    availableSlots: startTimes.map((startTime) => ({
      startTime: `${startTime}:00`,
      endTime: `${startTime.slice(0, 2)}:59:00`,
    })),
  };
}

function appointmentResponse(): AppointmentResponse {
  return {
    id: 99,
    startTime: `${TOMORROW}T09:00:00`,
    endTime: `${TOMORROW}T09:30:00`,
    doctorId: 10,
    doctorName: 'Dr. Gómez',
    doctorSpecialty: null,
    doctorEmail: 'gomez@clinic.test',
    patientId: 20,
    patientName: 'Ana Pérez',
    patientEmail: null,
    status: 'PENDING',
    cancellationReason: null,
  };
}

function apiError(status: number, body: Partial<ApiError>): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, message: 'Error', ...body } });
}

function activeConflictError(): HttpErrorResponse {
  return apiError(409, {
    code: API_ERROR_CODES.patientHasActive,
    message: 'El paciente ya tiene una cita activa con este doctor',
    details: { appointmentId: 7, startTime: `${TODAY}T08:00:00`, status: 'CONFIRMED' },
  });
}

describe('BookingService', () => {
  let service: BookingService;
  let appointmentsApi: { book: ReturnType<typeof vi.fn>; reschedule: ReturnType<typeof vi.fn> };
  let agendaApi: { availableSlots: ReturnType<typeof vi.fn> };
  let patientsApi: { list: ReturnType<typeof vi.fn> };
  let specialtiesStore: {
    specialties: WritableSignal<Specialty[]>;
    errorMessage: WritableSignal<string | null>;
    load: ReturnType<typeof vi.fn>;
    reload: ReturnType<typeof vi.fn>;
  };
  let doctorsStore: {
    doctors: WritableSignal<Staff[]>;
    isLoading: WritableSignal<boolean>;
    errorMessage: WritableSignal<string | null>;
    load: ReturnType<typeof vi.fn>;
    reload: ReturnType<typeof vi.fn>;
  };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let confirm: { ask: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function setup({ ownDataOnly = false, dateParam = null }: Setup = {}): void {
    appointmentsApi = { book: vi.fn(), reschedule: vi.fn() };
    agendaApi = { availableSlots: vi.fn(() => of(slots('09:00', '10:00'))) };
    patientsApi = { list: vi.fn(() => of(page([patient()]))) };
    specialtiesStore = {
      specialties: signal<Specialty[]>([]),
      errorMessage: signal<string | null>(null),
      load: vi.fn(),
      reload: vi.fn(),
    };
    doctorsStore = {
      doctors: signal<Staff[]>([]),
      isLoading: signal(false),
      errorMessage: signal<string | null>(null),
      load: vi.fn(),
      reload: vi.fn(),
    };
    notifications = { success: vi.fn(), error: vi.fn() };
    confirm = { ask: vi.fn() };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };

    TestBed.configureTestingModule({
      providers: [
        BookingService,
        {
          provide: AuthService,
          useValue: {
            restrictedToOwnData: signal(ownDataOnly),
            userId: signal(ownDataOnly ? 10 : 1),
            displayName: signal('Dr. Gómez'),
          },
        },
        { provide: AppointmentsApi, useValue: appointmentsApi },
        { provide: AgendaApi, useValue: agendaApi },
        { provide: PatientsApi, useValue: patientsApi },
        { provide: SpecialtiesStore, useValue: specialtiesStore },
        { provide: DoctorsStore, useValue: doctorsStore },
        { provide: NotificationService, useValue: notifications },
        { provide: ConfirmService, useValue: confirm },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: new BehaviorSubject(
              convertToParamMap(dateParam ? { date: dateParam } : {}),
            ),
          },
        },
      ],
    });
    service = TestBed.inject(BookingService);
  }

  async function fillForm(): Promise<void> {
    service.selectDoctor(doctor());
    service.selectDate(TOMORROW);
    await settle();
    service.selectSlot(service.slots()[0]);
    service.selectPatient(patient());
  }

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('initial state', () => {
    it('loads specialties and doctors for staff who can book for any doctor', () => {
      setup();
      expect(service.ownDataOnly).toBe(false);
      expect(specialtiesStore.load).toHaveBeenCalledOnce();
      expect(doctorsStore.load).toHaveBeenCalledOnce();
      expect(service.selectedDoctor()).toBeNull();
    });

    it('preselects the logged doctor and skips catalogs when restricted to own data', () => {
      setup({ ownDataOnly: true });
      expect(specialtiesStore.load).not.toHaveBeenCalled();
      expect(doctorsStore.load).not.toHaveBeenCalled();
      expect(service.selectedDoctor()).toEqual({
        id: 10,
        name: 'Dr. Gómez',
        specialtyId: null,
        specialtyName: null,
      });
    });

    it('starts on today without a date query param', () => {
      setup();
      expect(service.selectedDate()).toBe(TODAY);
    });

    it('starts on a valid future date from the query param', () => {
      setup({ dateParam: TOMORROW });
      expect(service.selectedDate()).toBe(TOMORROW);
    });

    it.each(['2000-01-01', 'not-a-date', '2026-02-30'])('ignores the query param %s', (date) => {
      setup({ dateParam: date });
      expect(service.selectedDate()).toBe(TODAY);
    });

    it('cannot submit until doctor, slot and patient are selected', () => {
      setup();
      expect(service.canSubmit()).toBe(false);
    });
  });

  describe('patients', () => {
    it('loads active patients with an empty search on start', async () => {
      setup();
      await settle();
      expect(patientsApi.list).toHaveBeenCalledWith({
        search: '',
        isActive: true,
        size: 20,
        sort: 'id',
      });
      expect(service.filteredPatients()).toEqual([patient()]);
    });

    it('debounces and trims the search before querying', async () => {
      vi.useFakeTimers({
        toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'],
      });
      setup();
      await settle();
      vi.advanceTimersByTime(500);
      patientsApi.list.mockClear();

      service.setPatientSearch('an');
      TestBed.tick();
      service.setPatientSearch('  ana  ');
      TestBed.tick();
      vi.advanceTimersByTime(499);
      await settle();
      expect(patientsApi.list).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1);
      await settle();
      expect(patientsApi.list).toHaveBeenCalledOnce();
      expect(patientsApi.list).toHaveBeenCalledWith(expect.objectContaining({ search: 'ana' }));
      expect(service.patientSearch()).toBe('  ana  ');
    });

    it('does not query again when the trimmed search is unchanged', async () => {
      vi.useFakeTimers({
        toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'],
      });
      setup();
      await settle();
      vi.advanceTimersByTime(500);
      patientsApi.list.mockClear();

      service.setPatientSearch('   ');
      TestBed.tick();
      vi.advanceTimersByTime(500);
      await settle();

      expect(patientsApi.list).not.toHaveBeenCalled();
    });

    it('exposes a load error message when the search fails', async () => {
      setup();
      patientsApi.list.mockReturnValue(throwError(() => apiError(500, { message: '' })));
      service.reloadPatients();
      await settle();
      expect(service.patientsErrorMessage()).toBe('No se pudieron cargar los pacientes');
      expect(service.filteredPatients()).toEqual([]);
    });
  });

  describe('doctor and specialty selection', () => {
    const cardio = doctor({ id: 10, specialtyId: 1 });
    const derma = doctor({ id: 11, name: 'Dra. Ruiz', specialtyId: 2, specialtyName: 'Derma' });

    it('filters doctors by the selected specialty', () => {
      setup();
      doctorsStore.doctors.set([cardio, derma]);
      expect(service.doctors()).toEqual([cardio, derma]);

      service.selectSpecialty(2);
      expect(service.doctors()).toEqual([derma]);

      service.selectSpecialty(null);
      expect(service.doctors()).toEqual([cardio, derma]);
    });

    it('clears the selected doctor when choosing another specialty', () => {
      setup();
      service.selectDoctor(cardio);
      service.selectSpecialty(2);
      expect(service.selectedDoctor()).toBeNull();
    });

    it('keeps the selected doctor when choosing their specialty or clearing it', () => {
      setup();
      service.selectDoctor(cardio);
      service.selectSpecialty(1);
      expect(service.selectedDoctor()?.id).toBe(10);
      service.selectSpecialty(null);
      expect(service.selectedDoctor()?.id).toBe(10);
    });

    it('maps the selected doctor to a doctor option', () => {
      setup();
      service.selectDoctor(cardio);
      expect(service.selectedDoctor()).toEqual({
        id: 10,
        name: 'Dr. Gómez',
        specialtyId: 1,
        specialtyName: 'Cardiología',
      });
    });

    it('ignores doctor and specialty changes when restricted to own data', () => {
      setup({ ownDataOnly: true });
      service.selectSpecialty(2);
      service.selectDoctor(derma);
      expect(service.specialtyId()).toBeNull();
      expect(service.selectedDoctor()?.id).toBe(10);
    });

    it('delegates catalog reloads to the stores', () => {
      setup();
      service.reloadSpecialties();
      service.reloadDoctors();
      expect(specialtiesStore.reload).toHaveBeenCalledOnce();
      expect(doctorsStore.reload).toHaveBeenCalledOnce();
    });
  });

  describe('date and slots', () => {
    it('does not load slots until a doctor is selected', async () => {
      setup();
      await settle();
      expect(agendaApi.availableSlots).not.toHaveBeenCalled();
      expect(service.slots()).toEqual([]);
    });

    it('loads slots for the doctor and date with HH:mm times', async () => {
      setup();
      service.selectDoctor(doctor());
      service.selectDate(TOMORROW);
      await settle();
      expect(agendaApi.availableSlots).toHaveBeenLastCalledWith(10, TOMORROW);
      expect(service.slots()).toEqual([
        { startTime: '09:00', endTime: '09:59' },
        { startTime: '10:00', endTime: '10:59' },
      ]);
    });

    it('ignores past or empty dates', () => {
      setup();
      service.selectDate('2000-01-01');
      service.selectDate('');
      expect(service.selectedDate()).toBe(TODAY);
    });

    it('resets the selected slot when the date or doctor changes', async () => {
      setup();
      await fillForm();
      expect(service.selectedSlot()).not.toBeNull();

      service.selectDate(TODAY);
      expect(service.selectedSlot()).toBeNull();

      service.selectSlot({ startTime: '10:00', endTime: '10:59' });
      service.selectDoctor(doctor({ id: 11 }));
      expect(service.selectedSlot()).toBeNull();
    });

    it('exposes a slots error message', async () => {
      setup();
      agendaApi.availableSlots.mockReturnValue(throwError(() => new Error('offline')));
      service.selectDoctor(doctor());
      await settle();
      expect(service.slotsErrorMessage()).toBe('No se pudieron cargar los horarios');
    });

    it('enables submit when doctor, slot and patient are selected', async () => {
      setup();
      await fillForm();
      expect(service.canSubmit()).toBe(true);
    });
  });

  describe('book', () => {
    it('does nothing when the form is incomplete', () => {
      setup();
      service.book();
      expect(appointmentsApi.book).not.toHaveBeenCalled();
    });

    it('books the slot, notifies and navigates to the calendar on success', async () => {
      setup();
      await fillForm();
      const response = new Subject<AppointmentResponse>();
      appointmentsApi.book.mockReturnValue(response);

      service.book();

      expect(appointmentsApi.book).toHaveBeenCalledWith({
        doctorId: 10,
        patientId: 20,
        startTime: `${TOMORROW}T09:00:00`,
      });
      expect(service.submitting()).toBe(true);
      expect(service.canSubmit()).toBe(false);

      response.next(appointmentResponse());
      response.complete();

      expect(service.submitting()).toBe(false);
      expect(notifications.success).toHaveBeenCalledWith(
        'Cita agendada: queda pendiente de confirmación',
      );
      expect(router.navigate).toHaveBeenCalledWith(['/calendar'], {
        queryParams: { date: TOMORROW },
      });
    });

    it('ignores a second submit while the first one is in flight', async () => {
      setup();
      await fillForm();
      appointmentsApi.book.mockReturnValue(new Subject());

      service.book();
      service.book();

      expect(appointmentsApi.book).toHaveBeenCalledOnce();
    });

    it('shows the API message and keeps the slot on a generic failure', async () => {
      setup();
      await fillForm();
      agendaApi.availableSlots.mockClear();
      appointmentsApi.book.mockReturnValue(
        throwError(() => apiError(400, { message: 'Paciente inactivo' })),
      );

      service.book();
      await settle();

      expect(notifications.error).toHaveBeenCalledWith('Paciente inactivo');
      expect(service.selectedSlot()).toEqual({ startTime: '09:00', endTime: '09:59' });
      expect(agendaApi.availableSlots).not.toHaveBeenCalled();
      expect(service.submitting()).toBe(false);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('uses the booking fallback message when the error has no message', async () => {
      setup();
      await fillForm();
      appointmentsApi.book.mockReturnValue(throwError(() => new Error('boom')));

      service.book();

      expect(notifications.error).toHaveBeenCalledWith('No se pudo agendar la cita');
    });

    it.each([
      API_ERROR_CODES.slotTaken,
      API_ERROR_CODES.scheduleBlocked,
      API_ERROR_CODES.outOfJourney,
      API_ERROR_CODES.pastDate,
    ])('clears the slot and reloads slots when the slot is unavailable (%s)', async (code) => {
      setup();
      await fillForm();
      agendaApi.availableSlots.mockClear();
      appointmentsApi.book.mockReturnValue(
        throwError(() => apiError(409, { code, message: 'Horario no disponible' })),
      );

      service.book();
      await settle();

      expect(notifications.error).toHaveBeenCalledWith('Horario no disponible');
      expect(service.selectedSlot()).toBeNull();
      expect(agendaApi.availableSlots).toHaveBeenCalledOnce();
    });

    it('treats the guest active appointment error as a plain failure', async () => {
      setup();
      await fillForm();
      appointmentsApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, {
            code: API_ERROR_CODES.patientHasActiveGuest,
            message: 'Ya tienes una cita activa',
            details: { startTime: `${TODAY}T08:00:00`, status: 'PENDING' },
          }),
        ),
      );

      service.book();

      expect(confirm.ask).not.toHaveBeenCalled();
      expect(notifications.error).toHaveBeenCalledWith('Ya tienes una cita activa');
    });
  });

  describe('book with an active appointment conflict', () => {
    let answer: Subject<boolean>;

    async function bookWithConflict(): Promise<void> {
      setup();
      await fillForm();
      answer = new Subject<boolean>();
      confirm.ask.mockReturnValue(answer);
      appointmentsApi.book.mockReturnValue(throwError(() => activeConflictError()));
      service.book();
    }

    it('asks to reschedule the existing appointment', async () => {
      await bookWithConflict();

      expect(confirm.ask).toHaveBeenCalledOnce();
      const options = confirm.ask.mock.calls[0][0];
      expect(options.title).toBe('Paciente con cita activa');
      expect(options.confirmLabel).toBe('Reprogramar');
      expect(options.message).toContain('Ana Pérez ya tiene una cita confirmada con Dr. Gómez');
      expect(options.message).toContain('a las 08:00');
      expect(options.message).toContain('a las 09:00?');
      expect(notifications.error).not.toHaveBeenCalled();
    });

    it('re-enables submitting while the confirmation is open', async () => {
      await bookWithConflict();
      expect(service.submitting()).toBe(false);
    });

    it('does nothing more when the user declines', async () => {
      await bookWithConflict();

      answer.next(false);
      answer.complete();

      expect(appointmentsApi.reschedule).not.toHaveBeenCalled();
      expect(notifications.success).not.toHaveBeenCalled();
      expect(notifications.error).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
      expect(service.submitting()).toBe(false);
      expect(service.selectedSlot()).not.toBeNull();
    });

    it('reschedules the existing appointment and navigates when accepted', async () => {
      await bookWithConflict();
      const rescheduled = new Subject<AppointmentResponse>();
      appointmentsApi.reschedule.mockReturnValue(rescheduled);

      answer.next(true);
      answer.complete();

      expect(appointmentsApi.reschedule).toHaveBeenCalledWith(7, {
        startTime: `${TOMORROW}T09:00:00`,
      });
      expect(service.submitting()).toBe(true);

      rescheduled.next(appointmentResponse());
      rescheduled.complete();

      expect(service.submitting()).toBe(false);
      expect(notifications.success).toHaveBeenCalledWith(
        'Cita reprogramada: queda pendiente de confirmación',
      );
      expect(router.navigate).toHaveBeenCalledWith(['/calendar'], {
        queryParams: { date: TOMORROW },
      });
    });

    it('shows the reschedule fallback when rescheduling fails', async () => {
      await bookWithConflict();
      appointmentsApi.reschedule.mockReturnValue(throwError(() => new Error('boom')));

      answer.next(true);
      answer.complete();

      expect(notifications.error).toHaveBeenCalledWith('No se pudo reprogramar la cita');
      expect(notifications.success).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
      expect(service.submitting()).toBe(false);
      expect(service.selectedSlot()).not.toBeNull();
    });

    it('clears the slot and reloads slots when the reschedule slot is taken', async () => {
      await bookWithConflict();
      agendaApi.availableSlots.mockClear();
      appointmentsApi.reschedule.mockReturnValue(
        throwError(() =>
          apiError(409, { code: API_ERROR_CODES.slotTaken, message: 'Horario ocupado' }),
        ),
      );

      answer.next(true);
      answer.complete();
      await settle();

      expect(notifications.error).toHaveBeenCalledWith('Horario ocupado');
      expect(service.selectedSlot()).toBeNull();
      expect(agendaApi.availableSlots).toHaveBeenCalledOnce();
    });

    it('treats an incomplete conflict payload as a plain failure', async () => {
      setup();
      await fillForm();
      appointmentsApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, {
            code: API_ERROR_CODES.patientHasActive,
            message: 'El paciente ya tiene una cita activa',
            details: { startTime: `${TODAY}T08:00:00`, status: 'PENDING' },
          }),
        ),
      );

      service.book();

      expect(confirm.ask).not.toHaveBeenCalled();
      expect(notifications.error).toHaveBeenCalledWith('El paciente ya tiene una cita activa');
    });
  });

  describe('teardown', () => {
    it('stops listening to a pending booking when the service is destroyed', async () => {
      setup();
      await fillForm();
      const response = new Subject<AppointmentResponse>();
      appointmentsApi.book.mockReturnValue(response);
      service.book();

      TestBed.resetTestingModule();
      response.next(appointmentResponse());

      expect(response.observed).toBe(false);
      expect(notifications.success).not.toHaveBeenCalled();
    });
  });
});
