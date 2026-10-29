import {
  HttpErrorResponse,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { GuestSessionService } from '../auth/guest-session.service';

const GUEST_PATH = 'guest/';
const GUEST_SESSION_PATH = 'public/guest/session';

function hasPath(url: string, path: string): boolean {
  return url.startsWith(path) || url.startsWith(environment.apiUrl + path);
}

export function isGuestRequest(url: string): boolean {
  return hasPath(url, GUEST_PATH);
}

export const apiUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (/^(https?:)?\/\//.test(req.url) || req.url.startsWith('/')) return next(req);
  return next(req.clone({ url: environment.apiUrl + req.url }));
};

function withHeaders(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  const currentLang = localStorage.getItem('lang') || navigator.language.slice(0, 2) || 'es';
  const headers: Record<string, string> = { 'Accept-Language': currentLang };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return req.clone({ setHeaders: headers });
}

function guestRequest(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const guest = inject(GuestSessionService);
  const send = () => guest.ensureToken().pipe(switchMap((token) => next(withHeaders(req, token))));
  return send().pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== HttpStatusCode.Unauthorized) return throwError(() => error);
      guest.clear();
      return send().pipe(
        catchError((retryError: unknown) => {
          if (retryError instanceof HttpErrorResponse && retryError.status === 401)
            guest.fail(retryError);
          return throwError(() => retryError);
        }),
      );
    }),
  );
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (hasPath(req.url, GUEST_SESSION_PATH)) return next(withHeaders(req, null));
  if (isGuestRequest(req.url)) return guestRequest(req, next);

  const auth = inject(AuthService);
  const token = auth.token();
  return next(withHeaders(req, token)).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === HttpStatusCode.Unauthorized && token) auth.logout();
      return throwError(() => error);
    }),
  );
};
