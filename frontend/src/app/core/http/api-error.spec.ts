import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { signal } from '@angular/core';
import { ApiError } from '../models/api.models';
import {
  API_ERROR_CODES,
  activeAppointmentsConflict,
  apiErrorCode,
  apiErrorMessage,
  isRateLimited,
  isSlotUnavailableError,
  resourceError,
  retryAfterSeconds,
  staffActiveAppointmentConflict,
} from './api-error';

function httpError(
  status: number,
  error: unknown = null,
  headers?: HttpHeaders,
): HttpErrorResponse {
  return new HttpErrorResponse({ status, error, headers });
}

function apiError(status: number, body: Partial<ApiError>): HttpErrorResponse {
  return httpError(status, { status, message: 'Error', ...body });
}

describe('api-error', () => {
  describe('isRateLimited', () => {
    it('is true for an HTTP 429 response', () => {
      expect(isRateLimited(httpError(429))).toBe(true);
    });

    it('is false for other statuses and non HTTP errors', () => {
      expect(isRateLimited(httpError(409))).toBe(false);
      expect(isRateLimited(new Error('boom'))).toBe(false);
    });
  });

  describe('retryAfterSeconds', () => {
    it('reads the Retry-After header of a 429 rounded up', () => {
      const error = httpError(429, null, new HttpHeaders({ 'Retry-After': '12.2' }));
      expect(retryAfterSeconds(error)).toBe(13);
    });

    it('returns 0 for a missing or invalid header', () => {
      expect(retryAfterSeconds(httpError(429))).toBe(0);
      expect(
        retryAfterSeconds(httpError(429, null, new HttpHeaders({ 'Retry-After': 'soon' }))),
      ).toBe(0);
      expect(
        retryAfterSeconds(httpError(429, null, new HttpHeaders({ 'Retry-After': '-5' }))),
      ).toBe(0);
    });

    it('returns 0 when the error is not rate limited', () => {
      expect(
        retryAfterSeconds(httpError(500, null, new HttpHeaders({ 'Retry-After': '10' }))),
      ).toBe(0);
    });
  });

  describe('apiErrorMessage', () => {
    it('returns the fallback for non HTTP errors', () => {
      expect(apiErrorMessage(new Error('boom'), 'Falló')).toBe('Falló');
    });

    it('uses a default fallback when none is given', () => {
      expect(apiErrorMessage('x')).toBe('Ocurrió un error inesperado');
    });

    it('returns a connection message for status 0', () => {
      expect(apiErrorMessage(httpError(0), 'Falló')).toBe('No se pudo conectar con el servidor');
    });

    it('joins unique field validation messages', () => {
      const error = apiError(400, {
        message: 'Datos inválidos',
        errors: [
          { field: 'name', message: 'El nombre es obligatorio' },
          { field: 'ci', message: 'El CI es obligatorio' },
          { field: 'other', message: 'El nombre es obligatorio' },
          { field: 'empty', message: '' },
        ],
      });
      expect(apiErrorMessage(error)).toBe('El nombre es obligatorio. El CI es obligatorio');
    });

    it('returns the body message when there are no field errors', () => {
      expect(apiErrorMessage(apiError(409, { message: 'Horario ocupado', errors: null }))).toBe(
        'Horario ocupado',
      );
    });

    it('returns the fallback for a string body', () => {
      expect(apiErrorMessage(httpError(500, 'Internal'), 'Falló')).toBe('Falló');
    });

    it('returns the rate limit message for a 429 without body message', () => {
      expect(apiErrorMessage(httpError(429), 'Falló')).toBe(
        'Has realizado demasiadas solicitudes. Intenta de nuevo en unos minutos.',
      );
    });

    it('prefers the body message of a 429 when present', () => {
      expect(apiErrorMessage(apiError(429, { message: 'Espera un momento' }))).toBe(
        'Espera un momento',
      );
    });
  });

  describe('apiErrorCode', () => {
    it('returns the code of an ErrorResponse body', () => {
      expect(apiErrorCode(apiError(409, { code: API_ERROR_CODES.slotTaken }))).toBe(
        'appointment.slotTaken',
      );
    });

    it.each([
      ['a non HTTP error', new Error('boom')],
      ['a null body', httpError(409)],
      ['a string body', httpError(409, 'conflict')],
      ['an array body', httpError(409, [{ code: 'x' }])],
      ['a missing code', apiError(409, {})],
      ['an empty code', apiError(409, { code: '' })],
      ['a non string code', httpError(409, { code: 42 })],
    ])('returns null for %s', (_, error) => {
      expect(apiErrorCode(error)).toBeNull();
    });
  });

  describe('isSlotUnavailableError', () => {
    it.each([
      API_ERROR_CODES.slotTaken,
      API_ERROR_CODES.scheduleBlocked,
      API_ERROR_CODES.outOfJourney,
      API_ERROR_CODES.pastDate,
    ])('is true for %s', (code) => {
      expect(isSlotUnavailableError(apiError(409, { code }))).toBe(true);
    });

    it.each([
      API_ERROR_CODES.patientHasActive,
      API_ERROR_CODES.patientHasActiveGuest,
      API_ERROR_CODES.activeAppointmentsConflict,
    ])('is false for %s', (code) => {
      expect(isSlotUnavailableError(apiError(409, { code }))).toBe(false);
    });

    it('is false for a 409 without code', () => {
      expect(isSlotUnavailableError(apiError(409, {}))).toBe(false);
    });
  });

  describe('activeAppointmentsConflict', () => {
    const code = API_ERROR_CODES.activeAppointmentsConflict;

    it('returns the count from the details', () => {
      expect(activeAppointmentsConflict(apiError(409, { code, details: { count: 3 } }))).toBe(3);
    });

    it('returns 0 for a zero count', () => {
      expect(activeAppointmentsConflict(apiError(409, { code, details: { count: 0 } }))).toBe(0);
    });

    it.each([
      ['missing details', undefined],
      ['missing count', {}],
      ['string count', { count: '3' }],
      ['negative count', { count: -1 }],
      ['infinite count', { count: Infinity }],
      ['NaN count', { count: NaN }],
    ])('returns 0 for %s', (_, details) => {
      expect(activeAppointmentsConflict(apiError(409, { code, details }))).toBe(0);
    });

    it('returns 0 when details is an array', () => {
      expect(activeAppointmentsConflict(httpError(409, { code, details: [3] }))).toBe(0);
    });

    it('returns null for another code', () => {
      expect(
        activeAppointmentsConflict(
          apiError(409, { code: API_ERROR_CODES.slotTaken, details: { count: 2 } }),
        ),
      ).toBeNull();
    });

    it('does not parse the message anymore for a 409 without code', () => {
      expect(activeAppointmentsConflict(apiError(409, { message: 'Hay 2 citas activas' }))).toBe(
        null,
      );
    });
  });

  describe('staffActiveAppointmentConflict', () => {
    const code = API_ERROR_CODES.patientHasActive;
    const details = { appointmentId: 7, startTime: '2026-11-10T09:00:00', status: 'CONFIRMED' };

    it('returns the conflict for a complete staff payload', () => {
      expect(staffActiveAppointmentConflict(apiError(409, { code, details }))).toEqual({
        appointmentId: 7,
        startTime: '2026-11-10T09:00:00',
        status: 'CONFIRMED',
      });
    });

    it('accepts a pending status', () => {
      const result = staffActiveAppointmentConflict(
        apiError(409, { code, details: { ...details, status: 'PENDING' } }),
      );
      expect(result?.status).toBe('PENDING');
    });

    it('ignores extra details fields', () => {
      const result = staffActiveAppointmentConflict(
        apiError(409, { code, details: { ...details, doctorId: 3 } }),
      );
      expect(result).toEqual({
        appointmentId: 7,
        startTime: '2026-11-10T09:00:00',
        status: 'CONFIRMED',
      });
    });

    it('returns null for the guest variant even with an id', () => {
      expect(
        staffActiveAppointmentConflict(
          apiError(409, { code: API_ERROR_CODES.patientHasActiveGuest, details }),
        ),
      ).toBeNull();
    });

    it.each([
      ['missing appointmentId', { startTime: details.startTime, status: 'PENDING' }],
      ['string appointmentId', { ...details, appointmentId: '7' }],
      ['missing startTime', { appointmentId: 7, status: 'PENDING' }],
      ['cancelled status', { ...details, status: 'CANCELLED' }],
      ['unknown status', { ...details, status: 'DONE' }],
    ])('returns null for %s', (_, invalid) => {
      expect(staffActiveAppointmentConflict(apiError(409, { code, details: invalid }))).toBeNull();
    });

    it('returns null without details', () => {
      expect(staffActiveAppointmentConflict(apiError(409, { code }))).toBeNull();
    });

    it('returns null for a non HTTP error', () => {
      expect(staffActiveAppointmentConflict({ error: { code, details } })).toBeNull();
    });
  });

  describe('resourceError', () => {
    it('is null while the resource has no error', () => {
      const error = signal<unknown>(undefined);
      const message = resourceError({ error: () => error() }, 'No se pudo cargar');
      expect(message()).toBeNull();
    });

    it('maps the resource error to a user message and reacts to changes', () => {
      const error = signal<unknown>(undefined);
      const message = resourceError({ error: () => error() }, 'No se pudo cargar');

      error.set(apiError(500, { message: 'Error del servidor' }));
      expect(message()).toBe('Error del servidor');

      error.set(new Error('network'));
      expect(message()).toBe('No se pudo cargar');

      error.set(undefined);
      expect(message()).toBeNull();
    });
  });
});
