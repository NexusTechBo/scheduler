import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { ApplicationRef, WritableSignal, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { GuestApi } from '../../core/api/guest.api';
import { GuestSessionService } from '../../core/auth/guest-session.service';
import { API_ERROR_CODES } from '../../core/http/api-error';
import {
  ApiError,
  AvailableSlots,
  GuestAppointmentResponse,
  GuestBookingPatient,
  GuestDoctor,
} from '../../core/models/api.models';
import { addDays, toIso } from '../../core/utils/calendar.util';
import { GuestBookingService } from './guest-booking.service';

const TODAY = toIso(new Date());
const TOMORROW = toIso(addDays(new Date(), 1));

const PATIENT: GuestBookingPatient = { ci: '1234567', phoneNumber: '70000000' };

function guestDoctor(overrides: Partial<GuestDoctor> = {}): GuestDoctor {
  return { id: 10, name: 'Dr. Gómez', specialtyId: 1, specialtyName: 'Cardiología', ...overrides };
}

function slots(...startTimes: string[]): AvailableSlots {
  return {
    date: TODAY,
    doctorId: 10,
    availableSlots: startTimes.map((startTime) => ({
      startTime: `${startTime}:00`,
      endTime: `${startTime.slice(0, 2)}:30:00`,
    })),
  };
}

function result(): GuestAppointmentResponse {
  return {
    appointmentId: 99,
    doctorName: 'Dr. Gómez',
    specialtyName: 'Cardiología',
    startTime: `${TODAY}T09:00:00`,
    endTime: `${TODAY}T09:30:00`,
    status: 'PENDING',
  };
}

function apiError(status: number, body: Partial<ApiError>, headers?: HttpHeaders) {
  return new HttpErrorResponse({ status, headers, error: { status, message: 'Error', ...body } });
}

describe('GuestBookingService', () => {
  let service: GuestBookingService;
  let guestApi: {
    clinic: ReturnType<typeof vi.fn>;
    specialties: ReturnType<typeof vi.fn>;
    doctors: ReturnType<typeof vi.fn>;
    availableSlots: ReturnType<typeof vi.fn>;
    book: ReturnType<typeof vi.fn>;
  };
  let ready: WritableSignal<boolean>;

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function setup(sessionReady = true): void {
    ready = signal(sessionReady);
    guestApi = {
      clinic: vi.fn(() => of({ name: 'Clínica Sur', slug: 'sur' })),
      specialties: vi.fn(() =>
        of([
          { id: 2, name: 'Pediatría' },
          { id: 1, name: 'Cardiología' },
        ]),
      ),
      doctors: vi.fn(() => of([guestDoctor()])),
      availableSlots: vi.fn(() => of(slots('09:00', '10:00'))),
      book: vi.fn(() => of(result())),
    };
    TestBed.configureTestingModule({
      providers: [
        GuestBookingService,
        { provide: GuestApi, useValue: guestApi },
        { provide: GuestSessionService, useValue: { ready } },
      ],
    });
    service = TestBed.inject(GuestBookingService);
  }

  async function reachSummary(): Promise<void> {
    await settle();
    service.selectDoctor(guestDoctor());
    await settle();
    service.selectSlot(service.slots()[0]);
    service.setPatient(PATIENT);
  }

  afterEach(() => vi.useRealTimers());

  describe('catalogs', () => {
    it('waits for the guest session before loading anything', async () => {
      setup(false);
      await settle();
      expect(guestApi.clinic).not.toHaveBeenCalled();
      expect(guestApi.specialties).not.toHaveBeenCalled();
      expect(guestApi.doctors).not.toHaveBeenCalled();
      expect(service.clinicName()).toBeNull();
    });

    it('loads clinic, sorted specialties and doctors once the session is ready', async () => {
      setup(false);
      ready.set(true);
      await settle();
      expect(service.clinicName()).toBe('Clínica Sur');
      expect(service.specialties().map((s) => s.name)).toEqual(['Cardiología', 'Pediatría']);
      expect(guestApi.doctors).toHaveBeenCalledWith(undefined);
      expect(service.doctors()).toEqual([guestDoctor()]);
    });

    it('filters doctors by specialty', async () => {
      setup();
      await settle();
      service.selectSpecialty(2);
      await settle();
      expect(guestApi.doctors).toHaveBeenLastCalledWith(2);
    });

    it('exposes the clinic error message', async () => {
      setup();
      guestApi.clinic.mockReturnValue(throwError(() => new Error('x')));
      service.reloadClinic();
      await settle();
      expect(service.clinicErrorMessage()).toBe('No se pudo cargar la clínica');
    });
  });

  describe('wizard steps', () => {
    it('starts on the doctor step', () => {
      setup();
      expect(service.step()).toBe('doctor');
      expect(service.stepNumber()).toBe(1);
      expect(service.totalSteps).toBe(4);
    });

    it('advances from doctor to slot, patient and summary', async () => {
      setup();
      await reachSummary();
      expect(service.step()).toBe('summary');
      expect(service.stepNumber()).toBe(4);
      expect(service.patient()).toEqual(PATIENT);
    });

    it('loads formatted slots for the selected doctor and date', async () => {
      setup();
      await settle();
      service.selectDoctor(guestDoctor());
      service.selectDate(TOMORROW);
      await settle();
      expect(guestApi.availableSlots).toHaveBeenLastCalledWith(10, TOMORROW);
      expect(service.slots()).toEqual([
        { startTime: '09:00', endTime: '09:30' },
        { startTime: '10:00', endTime: '10:30' },
      ]);
    });

    it('ignores past dates', () => {
      setup();
      service.selectDate('2000-01-01');
      expect(service.selectedDate()).toBe(TODAY);
    });

    it('goes back one step', async () => {
      setup();
      await reachSummary();
      service.back();
      expect(service.step()).toBe('patient');
    });

    it('does not go back from the first step or from done', async () => {
      setup();
      service.back();
      expect(service.step()).toBe('doctor');

      await reachSummary();
      service.book();
      expect(service.step()).toBe('done');
      service.back();
      expect(service.step()).toBe('done');
    });

    it('clears the doctor when switching to another specialty', () => {
      setup();
      service.selectDoctor(guestDoctor({ specialtyId: 1 }));
      service.selectSpecialty(2);
      expect(service.selectedDoctor()).toBeNull();
    });

    it('keeps the doctor when selecting their specialty', () => {
      setup();
      service.selectDoctor(guestDoctor({ specialtyId: 1 }));
      service.selectSpecialty(1);
      expect(service.selectedDoctor()?.id).toBe(10);
    });

    it('resets the selection when booking another appointment', async () => {
      setup();
      await reachSummary();
      service.book();

      service.bookAnother();

      expect(service.step()).toBe('doctor');
      expect(service.result()).toBeNull();
      expect(service.selectedDoctor()).toBeNull();
      expect(service.selectedSlot()).toBeNull();
      expect(service.selectedDate()).toBe(TODAY);
    });
  });

  describe('book', () => {
    it('does nothing without a complete selection', () => {
      setup();
      service.book();
      expect(guestApi.book).not.toHaveBeenCalled();
    });

    it('books and shows the result on success', async () => {
      setup();
      await reachSummary();

      service.book();

      expect(guestApi.book).toHaveBeenCalledWith({
        doctorId: 10,
        startTime: `${TODAY}T09:00:00`,
        patient: PATIENT,
      });
      expect(service.result()).toEqual(result());
      expect(service.step()).toBe('done');
      expect(service.submitting()).toBe(false);
      expect(service.error()).toBeNull();
    });

    it('ignores a second submit while booking', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(new Subject());

      service.book();
      service.book();

      expect(service.submitting()).toBe(true);
      expect(guestApi.book).toHaveBeenCalledOnce();
    });

    it('shows the API message and stays on the summary for a generic error', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(
        throwError(() => apiError(400, { message: 'Teléfono inválido' })),
      );

      service.book();

      expect(service.error()).toBe('Teléfono inválido');
      expect(service.step()).toBe('summary');
      expect(service.submitting()).toBe(false);
    });

    it('treats the guest active appointment conflict as a plain error', async () => {
      setup();
      await reachSummary();
      guestApi.availableSlots.mockClear();
      guestApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, {
            code: API_ERROR_CODES.patientHasActiveGuest,
            message: 'Ya tienes una cita activa con este doctor',
            details: { startTime: `${TOMORROW}T09:00:00`, status: 'PENDING' },
          }),
        ),
      );

      service.book();

      expect(service.error()).toBe('Ya tienes una cita activa con este doctor');
      expect(service.step()).toBe('summary');
      expect(guestApi.availableSlots).not.toHaveBeenCalled();
    });

    it('treats a 409 without a slot code as a plain error', async () => {
      setup();
      await reachSummary();
      guestApi.availableSlots.mockClear();
      guestApi.book.mockReturnValue(throwError(() => apiError(409, { message: 'Conflicto' })));

      service.book();

      expect(service.error()).toBe('Conflicto');
      expect(service.step()).toBe('summary');
      expect(guestApi.availableSlots).not.toHaveBeenCalled();
    });

    it('returns to the slot step and clears the slot when it was taken', async () => {
      setup();
      await reachSummary();
      guestApi.availableSlots.mockReturnValue(of(slots('10:00')));
      guestApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, { code: API_ERROR_CODES.slotTaken, message: 'Horario ocupado' }),
        ),
      );

      service.book();

      expect(service.step()).toBe('slot');
      expect(service.error()).toBe('Horario ocupado');
      expect(service.selectedSlot()).toBeNull();
      expect(service.slots()).toEqual([{ startTime: '10:00', endTime: '10:30' }]);
    });

    it('returns to the patient step keeping the slot when it is still free', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, { code: API_ERROR_CODES.outOfJourney, message: 'Fuera de horario' }),
        ),
      );

      service.book();

      expect(service.step()).toBe('patient');
      expect(service.error()).toBe('Fuera de horario');
      expect(service.selectedSlot()).toEqual({ startTime: '09:00', endTime: '09:30' });
    });

    it('returns to the slot step when refreshing the slots also fails', async () => {
      setup();
      await reachSummary();
      guestApi.availableSlots.mockReturnValue(throwError(() => new Error('offline')));
      guestApi.book.mockReturnValue(
        throwError(() =>
          apiError(409, { code: API_ERROR_CODES.scheduleBlocked, message: 'Agenda bloqueada' }),
        ),
      );

      service.book();
      await settle();

      expect(service.step()).toBe('slot');
      expect(service.error()).toBe('Agenda bloqueada');
      expect(service.selectedSlot()).toBeNull();
    });

    it('ignores the slot refresh when the user changed doctor meanwhile', async () => {
      setup();
      await reachSummary();
      const refreshed = new Subject<AvailableSlots>();
      guestApi.availableSlots.mockReturnValue(refreshed);
      guestApi.book.mockReturnValue(
        throwError(() => apiError(409, { code: API_ERROR_CODES.slotTaken, message: 'Ocupado' })),
      );

      service.book();
      service.bookAnother();
      refreshed.next(slots('10:00'));
      refreshed.complete();

      expect(service.step()).toBe('doctor');
      expect(service.error()).toBeNull();
    });
  });

  describe('rate limiting', () => {
    beforeEach(() => {
      vi.useFakeTimers({
        toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'],
      });
    });

    function rateLimited() {
      return apiError(
        429,
        { message: 'Demasiadas reservas' },
        new HttpHeaders({ 'Retry-After': '3' }),
      );
    }

    it('starts a cooldown and shows the message on the summary', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(throwError(() => rateLimited()));

      service.book();

      expect(service.coolingDown()).toBe(true);
      expect(service.retryInLabel()).toBe('3 s');
      expect(service.error()).toBe('Demasiadas reservas');
      expect(service.step()).toBe('summary');
    });

    it('blocks booking during the cooldown and allows it afterwards', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(throwError(() => rateLimited()));
      service.book();
      guestApi.book.mockClear();
      guestApi.book.mockReturnValue(of(result()));

      service.book();
      expect(guestApi.book).not.toHaveBeenCalled();

      vi.advanceTimersByTime(3000);
      expect(service.coolingDown()).toBe(false);
      expect(service.cooldownFinished()).toBe(true);
      expect(service.error()).toBeNull();

      service.book();
      expect(guestApi.book).toHaveBeenCalledOnce();
      expect(service.step()).toBe('done');
    });

    it('hides the rate limit message outside the summary step', async () => {
      setup();
      await reachSummary();
      guestApi.book.mockReturnValue(throwError(() => rateLimited()));
      service.book();

      service.back();

      expect(service.coolingDown()).toBe(true);
      expect(service.error()).toBeNull();
    });
  });
});
