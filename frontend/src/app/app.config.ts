import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { DEFAULT_DIALOG_CONFIG, DialogConfig } from '@angular/cdk/dialog';

import { routes } from './app.routes';
import { apiUrlInterceptor, authInterceptor } from './core/http/api.interceptors';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiUrlInterceptor, authInterceptor])),
    { provide: LOCALE_ID, useValue: 'es' },
    {
      provide: DEFAULT_DIALOG_CONFIG,
      useValue: { ...new DialogConfig(), backdropClass: 'glass-backdrop', ariaModal: true },
    },
  ],
};
