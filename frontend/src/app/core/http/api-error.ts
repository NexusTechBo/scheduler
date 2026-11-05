import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { computed, Signal } from '@angular/core';
import {
  ActiveAppointmentStatus,
  ApiError,
  StaffActiveAppointmentConflict,
} from '../models/api.models';

const RATE_LIMIT_MESSAGE =
  'Has realizado demasiadas solicitudes. Intenta de nuevo en unos minutos.';

export function isRateLimited(error: unknown): error is HttpErrorResponse {
  return error instanceof HttpErrorResponse && error.status === HttpStatusCode.TooManyRequests;
}

export function retryAfterSeconds(error: unknown): number {
  if (!isRateLimited(error)) return 0;
  const seconds = Number(error.headers.get('Retry-After'));
  return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : 0;
}

export function apiErrorMessage(error: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 0) return 'No se pudo conectar con el servidor';
  const body = error.error as Partial<ApiError> | string | null;
  if (body && typeof body === 'object') {
    const details = [...new Set((body.errors ?? []).map((e) => e.message).filter(Boolean))];
    if (details.length) return details.join('. ');
    if (body.message) return body.message;
  }
  return isRateLimited(error) ? RATE_LIMIT_MESSAGE : fallback;
}

export const API_ERROR_CODES = {
  slotTaken: 'appointment.slotTaken',
  scheduleBlocked: 'appointment.scheduleBlocked',
  outOfJourney: 'appointment.outOfJourney',
  pastDate: 'appointment.pastDate',
  patientHasActive: 'appointment.patientHasActive',
  patientHasActiveGuest: 'appointment.patientHasActiveGuest',
  activeAppointmentsConflict: 'scheduleException.activeAppointmentsConflict',
} as const;

const SLOT_UNAVAILABLE_CODES: ReadonlySet<string> = new Set([
  API_ERROR_CODES.slotTaken,
  API_ERROR_CODES.scheduleBlocked,
  API_ERROR_CODES.outOfJourney,
  API_ERROR_CODES.pastDate,
]);

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function isActiveStatus(value: unknown): value is ActiveAppointmentStatus {
  return value === 'PENDING' || value === 'CONFIRMED';
}

function errorBody(error: unknown): Record<string, unknown> | null {
  if (!(error instanceof HttpErrorResponse)) return null;
  const body: unknown = error.error;
  return isRecord(body) ? body : null;
}

export function apiErrorCode(error: unknown): string | null {
  const code = errorBody(error)?.['code'];
  return typeof code === 'string' && code ? code : null;
}

function apiErrorDetails(error: unknown): Record<string, unknown> {
  const details = errorBody(error)?.['details'];
  return isRecord(details) ? details : {};
}

export function isSlotUnavailableError(error: unknown): boolean {
  const code = apiErrorCode(error);
  return code !== null && SLOT_UNAVAILABLE_CODES.has(code);
}

export function activeAppointmentsConflict(error: unknown): number | null {
  if (apiErrorCode(error) !== API_ERROR_CODES.activeAppointmentsConflict) return null;
  const count = apiErrorDetails(error)['count'];
  // A missing or invalid count still means a conflict: 0 on purpose so callers show a generic text.
  return typeof count === 'number' && Number.isFinite(count) && count >= 0 ? count : 0;
}

/**
 * Staff-only conflict: the existing appointment id is required to offer a reschedule.
 * The guest variant (`patientHasActiveGuest`) only carries `startTime`, so guests just show the message.
 */
export function staffActiveAppointmentConflict(
  error: unknown,
): StaffActiveAppointmentConflict | null {
  if (apiErrorCode(error) !== API_ERROR_CODES.patientHasActive) return null;
  const { appointmentId, startTime, status } = apiErrorDetails(error);
  if (typeof appointmentId !== 'number' || typeof startTime !== 'string' || !isActiveStatus(status))
    return null;
  return { appointmentId, startTime, status };
}

export function resourceError(
  resource: { error(): unknown },
  fallback: string,
): Signal<string | null> {
  return computed(() => {
    const error = resource.error();
    return error ? apiErrorMessage(error, fallback) : null;
  });
}
