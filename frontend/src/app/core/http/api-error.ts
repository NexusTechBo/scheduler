import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Signal, computed } from '@angular/core';
import { ApiError } from '../models/api.models';

export function apiErrorMessage(error: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;
  if (error.status === 0) return 'No se pudo conectar con el servidor';

  const body = error.error as Partial<ApiError> | string | null;
  if (body && typeof body === 'object') {
    const details = [...new Set((body.errors ?? []).map((e) => e.message).filter(Boolean))];
    if (details.length) return details.join('. ');
    if (body.message) return body.message;
  }
  return fallback;
}

export function activeAppointmentsConflict(error: unknown): number | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.Conflict)
    return null;
  const count = (error.error as Partial<ApiError> | null)?.message?.match(/\d+/);
  return count ? Number(count[0]) : 0;
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
