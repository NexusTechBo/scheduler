import { Role } from '../models/api.models';
import { UiIconName } from '../../shared/ui/ui-icon/ui-icon.component';

export interface NavItem {
  label: string;
  route: string;
  icon: UiIconName;
  roles: Role[];
}

/**
 * Role groups shared by routes (roleGuard) and the sidebar so both always agree.
 * They mirror the backend @PreAuthorize rules of each endpoint the page depends on.
 */
export const ROLES = {
  staff: ['ADMINISTRATOR', 'DOCTOR', 'ASSISTANT'],
  everyone: ['ADMINISTRATOR', 'DOCTOR', 'ASSISTANT', 'PATIENT'],
  clinical: ['DOCTOR', 'ASSISTANT'],
  booking: ['DOCTOR', 'ASSISTANT', 'PATIENT'],
  doctor: ['DOCTOR'],
  admin: ['ADMINISTRATOR'],
  patient: ['PATIENT']
} as const satisfies Record<string, readonly Role[]>;

export const NAV_ITEMS: NavItem[] = [
  { label: 'Inicio', route: '/dashboard', icon: 'home', roles: [...ROLES.staff] },
  { label: 'Mis citas', route: '/appointments', icon: 'list', roles: [...ROLES.patient] },
  { label: 'Agendar cita', route: '/appointments/new', icon: 'plus', roles: [...ROLES.patient] },
  { label: 'Tablero', route: '/board', icon: 'board', roles: [...ROLES.staff] },
  { label: 'Calendario', route: '/calendar', icon: 'calendar', roles: [...ROLES.everyone] },
  { label: 'Citas', route: '/appointments', icon: 'list', roles: [...ROLES.staff] },
  { label: 'Pacientes', route: '/patients', icon: 'patients', roles: [...ROLES.clinical] },
  { label: 'Mi disponibilidad', route: '/availability', icon: 'clock', roles: [...ROLES.doctor] },
  { label: 'Personal', route: '/staff', icon: 'staff', roles: [...ROLES.admin] },
  { label: 'Especialidades', route: '/specialties', icon: 'tag', roles: [...ROLES.admin] },
  { label: 'Asistente IA', route: '/assistant', icon: 'sparkles', roles: [...ROLES.patient] }
];

export function navItemsFor(role: Role | null): NavItem[] {
  return role ? NAV_ITEMS.filter((item) => item.roles.includes(role)) : [];
}
