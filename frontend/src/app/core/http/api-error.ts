import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Signal, computed } from '@angular/core';
import { ApiError } from '../models/api.models';

const getLang = (): 'es' | 'en' => {
  const saved = localStorage.getItem('lang') || navigator.language.slice(0, 2);
  return saved === 'en' ? 'en' : 'es';
};

export const STATUS_FALLBACK: Record<'network' | 'unexpected', Record<'es' | 'en', string>> = {
  network: {
    es: 'No se pudo conectar con el servidor',
    en: 'Cannot reach the server. Please check your internet connection',
  },
  unexpected: {
    es: 'Ocurrió un error inesperado',
    en: 'An unexpected error occurred',
  },
};

export function apiErrorMessage(error: unknown, fallback?: string): string {
  const lang = getLang();
  const defaultFallback = fallback ?? STATUS_FALLBACK.unexpected[lang];

  if (!(error instanceof HttpErrorResponse)) return defaultFallback;
  if (error.status === 0) return STATUS_FALLBACK.network[lang];

  const body = error.error as Partial<ApiError> | string | null;
  if (body && typeof body === 'object') {
    const details = [...new Set((body.errors ?? []).map((e) => e.message).filter(Boolean))];
    if (details.length) return details.join('. ');
    if (body.message) return body.message;
  }

  return defaultFallback;
}

export function activeAppointmentsConflict(error: unknown): string | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.Conflict) {
    return null;
  }
  const body = error.error as Partial<ApiError> | string | null;
  if (body && typeof body === 'object' && body.message) {
    return body.message;
  }
  return '';
}

export function resourceError(
  resource: { error(): unknown },
  fallback?: string,
): Signal<string | null> {
  return computed(() => {
    const error = resource.error();
    return error ? apiErrorMessage(error, fallback) : null;
  });
}
