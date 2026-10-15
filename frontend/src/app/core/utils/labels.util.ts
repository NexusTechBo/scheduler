import { AppointmentStatus, DayOfWeek, Role } from '../models/api.models';

export const ROLE_LABEL: Record<Role, string> = {
  ADMINISTRATOR: 'Administrador',
  DOCTOR: 'Doctor',
  ASSISTANT: 'Asistente',
  PATIENT: 'Paciente',
};

export const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Pendiente',
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
};

/** Tailwind classes per status — keep in sync with the board columns. */
export const STATUS_STYLE: Record<AppointmentStatus, { dot: string; badge: string }> = {
  PENDING: { dot: 'bg-tertiary', badge: 'bg-tertiary/10 text-tertiary' },
  CONFIRMED: { dot: 'bg-primary', badge: 'bg-primary/10 text-primary' },
  CANCELLED: { dot: 'bg-gray-400', badge: 'bg-gray-900/[0.06] text-gray-500' },
};

export const DAYS_OF_WEEK: { value: DayOfWeek; label: string; short: string }[] = [
  { value: 'MONDAY', label: 'Lunes', short: 'Lun' },
  { value: 'TUESDAY', label: 'Martes', short: 'Mar' },
  { value: 'WEDNESDAY', label: 'Miércoles', short: 'Mié' },
  { value: 'THURSDAY', label: 'Jueves', short: 'Jue' },
  { value: 'FRIDAY', label: 'Viernes', short: 'Vie' },
  { value: 'SATURDAY', label: 'Sábado', short: 'Sáb' },
  { value: 'SUNDAY', label: 'Domingo', short: 'Dom' },
];

/** JS Date#getDay() (0 = Sunday) → backend DayOfWeek. */
export function dayOfWeekOf(date: Date): DayOfWeek {
  return DAYS_OF_WEEK[(date.getDay() + 6) % 7].value;
}

export function initials(name: string | null | undefined): string {
  if (!name) return '?';
  const words = name
    .replace(/^(dr|dra)\.?\s+/i, '')
    .trim()
    .split(/\s+/);
  return ((words[0]?.[0] ?? '') + (words[1]?.[0] ?? '')).toUpperCase() || '?';
}
