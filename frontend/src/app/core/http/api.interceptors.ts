import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

export const apiUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (/^(https?:)?\/\//.test(req.url) || req.url.startsWith('/')) return next(req);
  return next(req.clone({ url: environment.apiUrl + req.url }));
};

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const savedLang = localStorage.getItem('lang') || navigator.language.slice(0, 2);
  const currentLang = savedLang === 'en' ? 'en' : 'es';

  const headers: Record<string, string> = {
    'Accept-Language': currentLang,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const request = req.clone({ setHeaders: headers });

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === HttpStatusCode.Unauthorized && token) auth.logout();
      return throwError(() => error);
    }),
  );
};
