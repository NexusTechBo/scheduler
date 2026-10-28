import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize, interval, take } from 'rxjs';
import { ClinicsApi } from '../../../../core/api/clinics.api';
import { apiErrorMessage } from '../../../../core/http/api-error';

export const RESEND_COOLDOWN_SECONDS = 60;

@Injectable()
export class ResendActivationService {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly destroyRef = inject(DestroyRef);

  private readonly _sending = signal(false);
  private readonly _remaining = signal(0);
  private readonly _sent = signal(false);
  private readonly _error = signal<string | null>(null);
  readonly sending = this._sending.asReadonly();
  readonly remaining = this._remaining.asReadonly();
  readonly sent = this._sent.asReadonly();
  readonly error = this._error.asReadonly();

  readonly canSend = computed(() => !this._sending() && this._remaining() === 0);

  private countdown?: Subscription;

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
        error: (error) => this._error.set(resendErrorMessage(error, emailEditable)),
      });
  }

  startCooldown(): void {
    this.countdown?.unsubscribe();
    this._remaining.set(RESEND_COOLDOWN_SECONDS);
    this.countdown = interval(1000)
      .pipe(take(RESEND_COOLDOWN_SECONDS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this._remaining.update((seconds) => seconds - 1));
  }
}

const RESEND_FALLBACK = 'No se pudo reenviar el correo, intenta de nuevo';

function resendErrorMessage(error: unknown, emailEditable: boolean): string {
  if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.BadRequest) {
    return emailEditable ? 'Ingresa un email válido' : RESEND_FALLBACK;
  }
  return apiErrorMessage(error, RESEND_FALLBACK);
}
