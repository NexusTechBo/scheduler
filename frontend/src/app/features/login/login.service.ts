import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage, retryAfterSeconds } from '../../core/http/api-error';
import { HOME_URL } from '../../core/navigation/navigation';
import { NotificationService } from '../../shared/services/notification.service';
import { createCooldown } from '../../core/utils/cooldown.util';

@Injectable()
export class LoginService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly cooldown = createCooldown();

  private readonly _submitting = signal(false);
  private readonly _pendingActivation = signal(false);
  readonly submitting = this._submitting.asReadonly();
  readonly pendingActivation = this._pendingActivation.asReadonly();
  readonly coolingDown = this.cooldown.active;
  readonly retryInLabel = this.cooldown.label;
  readonly cooldownFinished = this.cooldown.finished;

  login(username: string, password: string): void {
    if (this.submitting() || this.cooldown.active()) return;
    this._submitting.set(true);
    this._pendingActivation.set(false);
    this.cooldown.reset();
    this.auth
      .login(username, password)
      .pipe(finalize(() => this._submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(HOME_URL).finally(),
        error: (error) => {
          if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Forbidden) {
            this._pendingActivation.set(true);
            return;
          }
          const retryAfter = retryAfterSeconds(error);
          if (retryAfter) this.cooldown.start(retryAfter);
          this.notifications.error(apiErrorMessage(error, 'No se pudo iniciar sesión'));
        },
      });
  }
}
