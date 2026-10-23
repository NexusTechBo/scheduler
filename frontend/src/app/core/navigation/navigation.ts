import { Role } from '../models/api.models';

export const HOME_URL = '/dashboard';

type NavIcon = 'home' | 'board' | 'calendar' | 'list' | 'patients' | 'clock' | 'staff' | 'tag';

export interface NavItem {
  label: string;
  description: string;
  route: string;
  icon: NavIcon;
  roles: Role[];
}

export const ROLES = {
  staff: ['ADMINISTRATOR', 'DOCTOR', 'ASSISTANT'],
  doctor: ['DOCTOR'],
  admin: ['ADMINISTRATOR'],
} as const satisfies Record<string, readonly Role[]>;

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Inicio',
    description: 'Resumen de la actividad',
    route: HOME_URL,
    icon: 'home',
    roles: [...ROLES.staff],
  },
  {
    label: 'Tablero',
    description: 'Confirma o cancela citas pendientes',
    route: '/board',
    icon: 'board',
    roles: [...ROLES.staff],
  },
  {
    label: 'Calendario',
    description: 'Citas del mes, día por día',
    route: '/calendar',
    icon: 'calendar',
    roles: [...ROLES.staff],
  },
  {
    label: 'Citas',
    description: 'Historial y próximas citas',
    route: '/appointments',
    icon: 'list',
    roles: [...ROLES.staff],
  },
  {
    label: 'Pacientes',
    description: 'Registra y asigna pacientes',
    route: '/patients',
    icon: 'patients',
    roles: [...ROLES.staff],
  },
  {
    label: 'Mi disponibilidad',
    description: 'Horario semanal y bloqueos',
    route: '/availability',
    icon: 'clock',
    roles: [...ROLES.doctor],
  },
  {
    label: 'Personal',
    description: 'Doctores y asistentes de la clínica',
    route: '/staff',
    icon: 'staff',
    roles: [...ROLES.admin],
  },
  {
    label: 'Especialidades',
    description: 'Catálogo de especialidades',
    route: '/specialties',
    icon: 'tag',
    roles: [...ROLES.admin],
  },
];

export function navItemsFor(role: Role | null): NavItem[] {
  return role ? NAV_ITEMS.filter((item) => item.roles.includes(role)) : [];
}
