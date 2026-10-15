import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/api.models';
import { AuthService } from './auth.service';

/** Protects the app shell. Without a valid session the user starts from clinic selection. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? true : inject(Router).createUrlTree([auth.clinic() ? '/login' : '/clinic-options']);
};

/** Login / register / clinic selection are only for anonymous users. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? inject(Router).createUrlTree([auth.homeUrl()]) : true;
};

/** Usage: `canActivate: [roleGuard('DOCTOR', 'ASSISTANT')]`. Mirrors the backend @PreAuthorize rules. */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasRole(...roles) ? true : inject(Router).createUrlTree([auth.homeUrl()]);
  };
}

/** `redirectTo` for the empty child route of the shell. */
export function homeRedirect(): string {
  return inject(AuthService).homeUrl();
}
