import { AppointmentStatus, AppointmentSummaryItem } from '../models/api.models';
import { toLocalDateTime } from './calendar.util';

export function appointmentStartDateTime(appointment: AppointmentSummaryItem): string {
  return toLocalDateTime(appointment.appointmentDate, appointment.startTime);
}

export function hasAppointmentStarted(startTime: string): boolean {
  return new Date(startTime).getTime() <= Date.now();
}

export function canConfirmAppointment(status: AppointmentStatus, startTime: string): boolean {
  return status === 'PENDING' && !hasAppointmentStarted(startTime);
}

export function canCancelAppointment(status: AppointmentStatus, startTime: string): boolean {
  return status !== 'CANCELLED' && !hasAppointmentStarted(startTime);
}
