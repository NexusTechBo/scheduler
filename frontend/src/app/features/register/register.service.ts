import { Injectable, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ClinicsApi } from '../../core/api/clinics.api';
import { apiErrorMessage, retryAfterSeconds } from '../../core/http/api-error';
import { ClinicRequest } from '../../core/models/api.models';
import { createCooldown } from '../../core/utils/cooldown.util';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class RegisterService {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly notifications = inject(NotificationService);
  private readonly cooldown = createCooldown();

  private readonly _submitting = signal(false);
  private readonly _registeredEmail = signal<string | null>(null);
  readonly submitting = this._submitting.asReadonly();
  readonly registeredEmail = this._registeredEmail.asReadonly();
  readonly coolingDown = this.cooldown.active;
  readonly retryInLabel = this.cooldown.label;
  readonly cooldownFinished = this.cooldown.finished;

  register(request: ClinicRequest): void {
    if (this.submitting() || this.cooldown.active()) return;
    this._submitting.set(true);
    this.cooldown.reset();
    this.clinicsApi
      .create(request)
      .pipe(finalize(() => this._submitting.set(false)))
      .subscribe({
        next: (clinic) => this._registeredEmail.set(clinic.adminEmail),
        error: (error) => {
          const retryAfter = retryAfterSeconds(error);
          if (retryAfter) this.cooldown.start(retryAfter);
          this.notifications.error(
            apiErrorMessage(error, 'Ocurrió un error al registrar, intenta de nuevo'),
          );
        },
      });
  }
}
