import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ClinicsApi } from '../../../core/api/clinics.api';
import { apiErrorMessage, retryAfterSeconds } from '../../../core/http/api-error';
import { createCooldown } from '../../../core/utils/cooldown.util';
import { INVALID_EMAIL_MESSAGE } from '../../utils/validation-messages.util';

const RESEND_COOLDOWN_SECONDS = 60;

@Injectable()
export class ResendActivationService {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly cooldown = createCooldown();

  private readonly _sending = signal(false);
  private readonly _sent = signal(false);
  private readonly _error = signal<string | null>(null);
  readonly sending = this._sending.asReadonly();
  readonly remainingLabel = this.cooldown.label;
  readonly sent = this._sent.asReadonly();
  readonly error = this._error.asReadonly();

  readonly canSend = computed(() => !this._sending() && !this.cooldown.active());

  resend(email: string, emailEditable = true): void {
    if (!this.canSend()) return;
    this._sending.set(true);
    this._error.set(null);
    this.clinicsApi
      .resendActivation(email)
      .pipe(finalize(() => this._sending.set(false)))
      .subscribe({
        next: () => {
          this._sent.set(true);
          this.startCooldown();
        },
        error: (error) => {
          this._error.set(resendErrorMessage(error, emailEditable));
          const retryAfter = retryAfterSeconds(error);
          if (retryAfter) this.cooldown.start(retryAfter, () => this._error.set(null));
        },
      });
  }

  startCooldown(): void {
    this.cooldown.start(RESEND_COOLDOWN_SECONDS);
  }
}

const RESEND_FALLBACK = 'No se pudo reenviar el correo, intenta de nuevo';

function resendErrorMessage(error: unknown, emailEditable: boolean): string {
  if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.BadRequest) {
    return emailEditable ? INVALID_EMAIL_MESSAGE : RESEND_FALLBACK;
  }
  return apiErrorMessage(error, RESEND_FALLBACK);
}
