import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { ClinicsApi } from '../../core/api/clinics.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { ClinicRequest } from '../../core/models/api.models';
import { HOME_URL } from '../../core/navigation/navigation';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class RegisterService {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private readonly _submitting = signal(false);
  readonly submitting = this._submitting.asReadonly();

  register(request: ClinicRequest): void {
    if (this.submitting()) return;
    this._submitting.set(true);
    this.clinicsApi
      .create(request)
      .pipe(
        switchMap((clinic) => this.auth.login(clinic.adminUsername, request.adminPassword)),
        finalize(() => this._submitting.set(false)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('¡Clínica registrada! Ya puedes crear a tu personal.');
          this.router.navigateByUrl(HOME_URL).finally();
        },
        error: (error) =>
          this.notifications.error(
            apiErrorMessage(error, 'Ocurrió un error al registrar, intenta de nuevo'),
          ),
      });
  }
}
