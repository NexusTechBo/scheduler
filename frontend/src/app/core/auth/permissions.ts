import { AppointmentStatus } from '../models/api.models';

export function canConfirmAppointment(status: AppointmentStatus): boolean {
  return status === 'PENDING';
}

export function canCancelAppointment(status: AppointmentStatus): boolean {
  return status !== 'CANCELLED';
}
