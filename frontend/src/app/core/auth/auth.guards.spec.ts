import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Role } from '../models/api.models';
import { authGuard, guestGuard, roleGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('auth guards', () => {
  let authenticated: boolean;
  let roles: Role[];

  beforeEach(() => {
    authenticated = false;
    roles = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => authenticated,
            hasRole: (...wanted: Role[]) => wanted.some((role) => roles.includes(role)),
          },
        },
      ],
    });
  });

  function run(guard: CanActivateFn): true | string {
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    if (result === true) return true;
    if (result instanceof UrlTree) return result.toString();
    throw new Error(`Unexpected guard result: ${String(result)}`);
  }

  describe('authGuard', () => {
    it('allows an authenticated user', () => {
      authenticated = true;
      expect(run(authGuard)).toBe(true);
    });

    it('redirects an anonymous user to the login', () => {
      expect(run(authGuard)).toBe('/login');
    });
  });

  describe('guestGuard', () => {
    it('allows an anonymous user', () => {
      expect(run(guestGuard)).toBe(true);
    });

    it('redirects an authenticated user to the dashboard', () => {
      authenticated = true;
      expect(run(guestGuard)).toBe('/dashboard');
    });
  });

  describe('roleGuard', () => {
    it('allows a user with one of the roles', () => {
      roles = ['DOCTOR'];
      expect(run(roleGuard('ADMINISTRATOR', 'DOCTOR'))).toBe(true);
    });

    it('allows an administrator that is also a doctor on a doctor page', () => {
      roles = ['ADMINISTRATOR', 'DOCTOR'];
      expect(run(roleGuard('DOCTOR'))).toBe(true);
    });

    it('redirects a user without the role to the dashboard', () => {
      roles = ['ASSISTANT'];
      expect(run(roleGuard('ADMINISTRATOR'))).toBe('/dashboard');
    });
  });
});
