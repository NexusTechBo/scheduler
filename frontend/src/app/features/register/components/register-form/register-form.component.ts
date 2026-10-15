import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap, tap } from 'rxjs';
import { ClinicsApi } from '../../../../core/api/clinics.api';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { NotificationService } from '../../../../shared/services/notification.service';
import { fieldError } from '../../../../shared/utils/validation-messages.util';
import { REGISTER_FORM } from './constants/register-form.constants';

/** Registers a clinic (new tenant) with its administrator, then logs the administrator in. */
@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-form.component.html',
  host: { class: 'flex min-h-screen items-center justify-center px-4 py-10 sm:px-6' },
})
export class RegisterFormComponent {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  protected readonly submitting = signal(false);
  protected readonly formGroup = inject(FormBuilder).nonNullable.group(REGISTER_FORM);

  protected errorOf(name: keyof typeof this.formGroup.controls): string | null {
    return fieldError(this.formGroup.controls[name]);
  }

  protected submit(): void {
    if (this.submitting()) return;
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }

    const request = this.formGroup.getRawValue();
    this.submitting.set(true);
    this.clinicsApi
      .create(request)
      .pipe(
        tap((clinic) =>
          this.auth.selectClinic({
            id: clinic.id,
            name: clinic.name,
            phoneNumber: clinic.phoneNumber,
          }),
        ),
        switchMap((clinic) =>
          this.auth.login(request.adminEmail, request.adminPassword, clinic.id),
        ),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('¡Clínica registrada! Ya puedes crear a tu personal.');
          this.router.navigateByUrl(this.auth.homeUrl());
        },
        error: (error) =>
          this.notifications.error(
            apiErrorMessage(error, 'Ocurrió un error al registrar, intenta de nuevo'),
          ),
      });
  }
}
