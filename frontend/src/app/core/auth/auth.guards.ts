import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/api.models';
import { HOME_URL } from '../navigation/navigation';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  return inject(AuthService).token() ? true : inject(Router).createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () =>
  inject(AuthService).token() ? inject(Router).createUrlTree([HOME_URL]) : true;

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () =>
    inject(AuthService).hasRole(...roles) ? true : inject(Router).createUrlTree([HOME_URL]);
}
