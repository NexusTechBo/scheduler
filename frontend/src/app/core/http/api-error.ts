import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api.models';

const KNOWN_MESSAGES: [RegExp, string][] = [
  [/invalid credentials/i, 'Correo o contraseña incorrectos'],
  [/invalid clinic/i, 'La clínica seleccionada no existe, elige otra'],
  [/slot is already taken/i, 'Ese horario ya fue reservado, elige otro'],
  [/schedule is blocked/i, 'El doctor bloqueó ese horario'],
  [/out of journey|does not attend/i, 'El horario está fuera de la jornada del doctor'],
  [/overlaps an existing block/i, 'Ese horario se cruza con otro bloque de disponibilidad'],
  [/block date cannot be in the past/i, 'No se puede bloquear una fecha pasada'],
  [/in the past/i, 'No se puede agendar en una fecha pasada'],
  [/active appointments? in this range/i, 'Hay citas activas en ese horario'],
  [/doctor is not active/i, 'El doctor no está activo'],
  [/patient is not active/i, 'El paciente no está activo'],
  [/is not a doctor/i, 'El miembro del personal seleccionado no es doctor'],
  [/doctor not found/i, 'No se encontró el doctor'],
  [/patient not found/i, 'No se encontró el paciente'],
  [/just can confirm/i, 'Solo se pueden confirmar citas pendientes'],
  [/already cancelled/i, 'La cita ya estaba cancelada'],
  [/\b(email|ci)\b.*already exists/i, 'Ya existe un registro con ese correo o CI'],
  [/phone number.*already exists/i, 'Ese teléfono ya está registrado'],
  [/clinic with this name already exists/i, 'Ya existe una clínica con ese nombre'],
  [/user already exists/i, 'Ese usuario ya está registrado en la clínica'],
  [/already exists/i, 'Ya existe un registro con esos datos.'],
  [/doctor role should have a specialty/i, 'El doctor debe tener una especialidad'],
  [/assistant role shouldn't have a specialty/i, 'El asistente no debe tener especialidad'],
  [/role you have entered is not permitted/i, 'Rol no permitido'],
  [/end time must be after/i, 'La hora de fin debe ser posterior a la de inicio'],
  [/partial block needs/i, 'Indica hora de inicio y fin del bloqueo'],
  [/modified by another request/i, 'Otro usuario modificó este registro, vuelve a intentarlo'],
  [/not authorize|access denied/i, 'No tienes permiso para realizar esta acción'],
  [/must not be (blank|null|empty)/i, 'Completa los campos obligatorios'],
  [/well-formed email/i, 'Ingresa un email válido'],
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
    const details = new Set(
      (body.errors ?? []).map((e) => translate(e.message ?? '')).filter((m): m is string => !!m),
    );
    if (details.size) return [...details].join('. ');
    if (body.message) {
      const known = translate(body.message);
      if (known) return known;
    }
  }

  return STATUS_FALLBACK[error.status] ?? fallback;
}

export function activeAppointmentsConflict(error: unknown): number | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== 409) return null;
  const match = (error.error as Partial<ApiError> | null)?.message?.match(
    /(\d+) active appointments? in this range/,
  );
  return match ? Number(match[1]) : null;
}
