import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.models';

/**
 * The backend has no machine-readable error codes, only English messages (see GlobalExceptionHandler).
 * Known business messages are translated here; anything else falls back to a generic text per status.
 */
const KNOWN_MESSAGES: [RegExp, string][] = [
  [/invalid credentials/i, 'Correo o contraseña incorrectos'],
  [/slot is already taken/i, 'Ese horario ya fue reservado, elige otro'],
  [/schedule is blocked/i, 'El doctor bloqueó ese horario'],
  [/out of journey|does not attend/i, 'El horario está fuera de la jornada del doctor'],
  [/in the past/i, 'No se puede agendar en una fecha pasada'],
  [/just can confirm/i, 'Solo se pueden confirmar citas pendientes'],
  [/already cancelled/i, 'La cita ya estaba cancelada'],
  [/\b(email|ci)\b.*already exists/i, 'Ya existe un registro con ese correo o CI'],
  [/user already exists/i, 'Ese usuario ya está registrado en la clínica'],
  [/already exists/i, 'Ya existe un registro con esos datos.'],
  [/doctor role should have a specialty/i, 'El doctor debe tener una especialidad'],
  [/assistant role shouldn't have a specialty/i, 'El asistente no debe tener especialidad'],
  [/role you have entered is not permitted/i, 'Rol no permitido'],
  [/end time must be after/i, 'La hora de fin debe ser posterior a la de inicio'],
  [/partial block needs/i, 'Indica hora de inicio y fin del bloqueo'],
  [/modified by another request/i, 'Otro usuario modificó este registro, vuelve a intentarlo'],
  [/not authorize|access denied/i, 'No tienes permiso para realizar esta acción'],
];

const STATUS_FALLBACK: Record<number, string> = {
  0: 'No se pudo conectar con el servidor',
  400: 'Revisa los datos ingresados',
  401: 'Tu sesión expiró, vuelve a iniciar sesión',
  403: 'No tienes permiso para realizar esta acción',
  404: 'No se encontró el recurso solicitado',
  409: 'Conflicto con el estado actual, vuelve a intentarlo',
};

function translate(message: string): string | null {
  return KNOWN_MESSAGES.find(([pattern]) => pattern.test(message))?.[1] ?? null;
}

export function apiErrorMessage(error: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (!(error instanceof HttpErrorResponse)) return fallback;

  const body = error.error as Partial<ApiError> | string | null;
  if (body && typeof body === 'object') {
    const details = (body.errors ?? [])
      .map((e) => e.message)
      .filter(Boolean)
      .map((m) => translate(m) ?? m);
    if (details.length) return details.join('. ');
    if (body.message) {
      const known = translate(body.message);
      if (known) return known;
    }
  }

  return STATUS_FALLBACK[error.status] ?? fallback;
}
