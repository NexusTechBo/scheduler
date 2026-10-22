import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { HOME_URL } from '../../../core/navigation/navigation';
import { NotificationService } from '../../../shared/services/notification.service';

@Injectable()
export class LoginService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private readonly _submitting = signal(false);
  readonly submitting = this._submitting.asReadonly();

  login(username: string, password: string): void {
    if (this.submitting()) return;
    this._submitting.set(true);
    this.auth
      .login(username, password)
      .pipe(finalize(() => this._submitting.set(false)))
      .subscribe({
        next: () => this.router.navigateByUrl(HOME_URL).finally(),
        error: (error) =>
          this.notifications.error(apiErrorMessage(error, 'No se pudo iniciar sesión')),
      });
  }
}
