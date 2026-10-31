import { Role } from '../models/api.models';

export const ROLES = {
  staff: ['ADMINISTRATOR', 'DOCTOR', 'ASSISTANT'],
  doctor: ['DOCTOR'],
  admin: ['ADMINISTRATOR'],
  guestLink: ['ADMINISTRATOR'],
} as const satisfies Record<string, readonly Role[]>;
