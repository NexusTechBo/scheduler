import { AppointmentStatus, Role } from '../models/api.models';
import { ROLES } from '../navigation/navigation';

function hasAnyRole(role: Role | null, roles: readonly Role[]): boolean {
  return role !== null && roles.includes(role);
}

export function canManageAppointments(role: Role | null): boolean {
  return hasAnyRole(role, ROLES.staff);
}

export function canBookAppointments(role: Role | null): boolean {
  return hasAnyRole(role, ROLES.staff);
}

export function canConfirmAppointment(role: Role | null, status: AppointmentStatus): boolean {
  return canManageAppointments(role) && status === 'PENDING';
}

export function canCancelAppointment(role: Role | null, status: AppointmentStatus): boolean {
  return canManageAppointments(role) && status !== 'CANCELLED';
}
