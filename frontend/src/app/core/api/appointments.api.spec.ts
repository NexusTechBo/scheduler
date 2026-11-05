import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppointmentResponse } from '../models/api.models';
import { AppointmentsApi } from './appointments.api';

function appointment(overrides: Partial<AppointmentResponse> = {}): AppointmentResponse {
  return {
    id: 7,
    startTime: '2026-11-12T11:30:00',
    endTime: '2026-11-12T12:00:00',
    doctorId: 10,
    doctorName: 'Dr. Gómez',
    doctorSpecialty: null,
    doctorEmail: 'gomez@clinic.test',
    patientId: 20,
    patientName: 'Ana Pérez',
    patientEmail: null,
    status: 'PENDING',
    cancellationReason: null,
    ...overrides,
  };
}

describe('AppointmentsApi', () => {
  let api: AppointmentsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AppointmentsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('reschedule', () => {
    it('sends a PATCH with the new start time and returns the appointment', () => {
      let result: AppointmentResponse | undefined;
      api.reschedule(7, { startTime: '2026-11-12T11:30:00' }).subscribe((r) => (result = r));

      const req = http.expectOne('appointments/7/reschedule');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ startTime: '2026-11-12T11:30:00' });
      req.flush(appointment());

      expect(result?.id).toBe(7);
    });

    it('propagates the backend error', () => {
      let status: number | undefined;
      api.reschedule(7, { startTime: '2026-11-12T11:30:00' }).subscribe({
        error: (e) => (status = e.status),
      });

      http
        .expectOne('appointments/7/reschedule')
        .flush({ code: 'appointment.slotTaken' }, { status: 409, statusText: 'Conflict' });

      expect(status).toBe(409);
    });
  });

  describe('book', () => {
    it('sends a POST with the request body', () => {
      api.book({ doctorId: 10, patientId: 20, startTime: '2026-11-12T11:30:00' }).subscribe();

      const req = http.expectOne('appointments');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        doctorId: 10,
        patientId: 20,
        startTime: '2026-11-12T11:30:00',
      });
      req.flush(appointment());
    });
  });

  describe('confirm and cancel', () => {
    it('confirms with a PATCH without body', () => {
      api.confirm(7).subscribe();
      const req = http.expectOne('appointments/7/confirm');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toBeNull();
      req.flush(appointment({ status: 'CONFIRMED' }));
    });

    it('cancels with a PATCH and the reason', () => {
      api.cancel(7, { reason: 'Viaje' }).subscribe();
      const req = http.expectOne('appointments/7/cancel');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ reason: 'Viaje' });
      req.flush(appointment({ status: 'CANCELLED' }));
    });
  });

  describe('queries', () => {
    it('lists with filters as params, omitting empty values', () => {
      api.list({ doctorId: 10, status: 'PENDING', page: 0, patientId: undefined }).subscribe();
      const req = http.expectOne((r) => r.url === 'appointments');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('doctorId')).toBe('10');
      expect(req.request.params.get('status')).toBe('PENDING');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.has('patientId')).toBe(false);
      req.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
    });

    it('gets one appointment by id', () => {
      api.get(7).subscribe();
      const req = http.expectOne('appointments/7');
      expect(req.request.method).toBe('GET');
      req.flush(appointment());
    });

    it('loads the board for a range', () => {
      api.board({ from: '2026-11-05', to: '2026-11-11' }).subscribe();
      const req = http.expectOne((r) => r.url === 'appointments/board');
      expect(req.request.params.get('from')).toBe('2026-11-05');
      expect(req.request.params.get('to')).toBe('2026-11-11');
      req.flush({ PENDING: [], CONFIRMED: [], CANCELLED: [] });
    });

    it('loads the calendar for a month', () => {
      api.calendar({ month: 11, year: 2026, doctorId: 10 }).subscribe();
      const req = http.expectOne((r) => r.url === 'appointments/calendar');
      expect(req.request.params.get('month')).toBe('11');
      expect(req.request.params.get('year')).toBe('2026');
      expect(req.request.params.get('doctorId')).toBe('10');
      req.flush({});
    });
  });
});
