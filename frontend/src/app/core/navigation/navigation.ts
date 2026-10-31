import { ROLES } from '../auth/roles';
import { Role } from '../models/api.models';

export const HOME_URL = '/dashboard';

export const GUEST_BOOKING_PATH = 'clinic/guest/appointment/clinic';

export const PAGE_TITLES = {
  dashboard: 'Inicio',
  board: 'Tablero',
  calendar: 'Calendario',
  appointments: 'Citas',
  booking: 'Agendar cita',
  patients: 'Pacientes',
  availability: 'Mi disponibilidad',
  staff: 'Personal',
  specialties: 'Especialidades',
  profile: 'Mi perfil',
} as const;

type NavIcon = 'home' | 'board' | 'calendar' | 'list' | 'patients' | 'clock' | 'staff' | 'tag';

export interface NavItem {
  label: string;
  description: string;
  route: string;
  icon: NavIcon;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  {
    label: PAGE_TITLES.dashboard,
    description: 'Resumen de la actividad',
    route: HOME_URL,
    icon: 'home',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.board,
    description: 'Confirma o cancela citas pendientes',
    route: '/board',
    icon: 'board',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.calendar,
    description: 'Citas del mes, día por día',
    route: '/calendar',
    icon: 'calendar',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.appointments,
    description: 'Historial y próximas citas',
    route: '/appointments',
    icon: 'list',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.patients,
    description: 'Registra y asigna pacientes',
    route: '/patients',
    icon: 'patients',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.availability,
    description: 'Horario semanal y bloqueos',
    route: '/availability',
    icon: 'clock',
    roles: [...ROLES.doctor],
  },
  {
    label: PAGE_TITLES.staff,
    description: 'Doctores y asistentes de la clínica',
    route: '/staff',
    icon: 'staff',
    roles: [...ROLES.admin],
  },
  {
    label: PAGE_TITLES.specialties,
    description: 'Catálogo de especialidades',
    route: '/specialties',
    icon: 'tag',
    roles: [...ROLES.admin],
  },
];

export function navItemsFor(role: Role | null): NavItem[] {
  return role ? NAV_ITEMS.filter((item) => item.roles.includes(role)) : [];
}
