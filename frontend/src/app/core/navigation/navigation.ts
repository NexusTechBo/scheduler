import { ROLES } from '../auth/roles';
import { Role } from '../models/api.models';

export type SessionAccess = 'required' | 'guestOnly';

type NavIcon = 'home' | 'board' | 'calendar' | 'list' | 'patients' | 'clock' | 'staff' | 'tag';

export interface NavItem {
  label: string;
  description: string;
  route: string;
  icon: NavIcon;
  roles: Role[];
}

export const HOME_URL = '/dashboard';

export const GUEST_BOOKING_PATH = 'clinic/guest/appointment/clinic';

export const SESSION_ACCESS = 'sessionAccess';

export const PAGE_TITLES = {
  dashboard: 'nav.dashboard',
  board: 'nav.board',
  calendar: 'nav.calendar',
  appointments: 'nav.appointments',
  booking: 'nav.booking',
  patients: 'nav.patients',
  availability: 'nav.availability',
  staff: 'nav.staff',
  specialties: 'nav.specialties',
  profile: 'nav.profile',
} as const;

const NAV_ITEMS: NavItem[] = [
  {
    label: PAGE_TITLES.dashboard,
    description: 'nav.descriptions.dashboard',
    route: HOME_URL,
    icon: 'home',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.board,
    description: 'nav.descriptions.board',
    route: '/board',
    icon: 'board',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.calendar,
    description: 'nav.descriptions.calendar',
    route: '/calendar',
    icon: 'calendar',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.appointments,
    description: 'nav.descriptions.appointments',
    route: '/appointments',
    icon: 'list',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.patients,
    description: 'nav.descriptions.patients',
    route: '/patients',
    icon: 'patients',
    roles: [...ROLES.staff],
  },
  {
    label: PAGE_TITLES.availability,
    description: 'nav.descriptions.availability',
    route: '/availability',
    icon: 'clock',
    roles: [...ROLES.doctor],
  },
  {
    label: PAGE_TITLES.staff,
    description: 'nav.descriptions.staff',
    route: '/staff',
    icon: 'staff',
    roles: [...ROLES.admin],
  },
  {
    label: PAGE_TITLES.specialties,
    description: 'nav.descriptions.specialties',
    route: '/specialties',
    icon: 'tag',
    roles: [...ROLES.admin],
  },
];

export function navItemsFor(roles: readonly Role[]): NavItem[] {
  return NAV_ITEMS.filter((item) => item.roles.some((role) => roles.includes(role)));
}
