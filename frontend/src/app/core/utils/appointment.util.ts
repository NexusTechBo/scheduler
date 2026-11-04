import { AppointmentStatus } from '../models/api.models';

export function hasAppointmentStarted(startTime: string): boolean {
  return new Date(startTime).getTime() <= Date.now();
}

export function canConfirmAppointment(status: AppointmentStatus, startTime: string): boolean {
  return status === 'PENDING' && !hasAppointmentStarted(startTime);
}

export function canCancelAppointment(status: AppointmentStatus, startTime: string): boolean {
  return status !== 'CANCELLED' && !hasAppointmentStarted(startTime);
}
