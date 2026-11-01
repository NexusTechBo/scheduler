import { DEFAULT_DIALOG_CONFIG, DialogConfig } from '@angular/cdk/dialog';
import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import localeEs from '@angular/common/locales/es';
import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { apiUrlInterceptor, authInterceptor } from './core/http/api.interceptors';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withXhr(), withInterceptors([apiUrlInterceptor, authInterceptor])),
    { provide: LOCALE_ID, useValue: 'es' },
    {
      provide: DEFAULT_DIALOG_CONFIG,
      useValue: { ...new DialogConfig(), backdropClass: 'glass-backdrop', ariaModal: true },
    },
  ],
};
