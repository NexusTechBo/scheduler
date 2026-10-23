import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { HOME_URL } from '../../core/navigation/navigation';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class LoginService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private readonly _submitting = signal(false);
  private readonly _pendingActivation = signal(false);
  readonly submitting = this._submitting.asReadonly();
  /** True when the credentials are right but the clinic has not been activated yet. */
  readonly pendingActivation = this._pendingActivation.asReadonly();

  login(username: string, password: string): void {
    if (this.submitting()) return;
    this._submitting.set(true);
    this._pendingActivation.set(false);
    this.auth
      .login(username, password)
      .pipe(finalize(() => this._submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(HOME_URL).finally(),
        error: (error) => {
          // auth/login only answers 403 for a clinic pending activation.
          if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Forbidden) {
            this._pendingActivation.set(true);
            return;
          }
          this.notifications.error(apiErrorMessage(error, 'No se pudo iniciar sesión'));
        },
      });
  }
}
