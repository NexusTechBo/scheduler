import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/api.models';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token()
    ? true
    : inject(Router).createUrlTree([auth.clinic() ? '/login' : '/clinic-options']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? inject(Router).createUrlTree([auth.homeUrl()]) : true;
};

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasRole(...roles) ? true : inject(Router).createUrlTree([auth.homeUrl()]);
  };
}

export function homeRedirect(): string {
  return inject(AuthService).homeUrl();
}
