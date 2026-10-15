import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { AuthApi } from '../../core/api/auth.api';
import { PatientsApi } from '../../core/api/patients.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { ROLE_LABEL, initials } from '../../core/utils/labels.util';
import { NotificationService } from '../../shared/services/notification.service';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { fieldError } from '../../shared/utils/validation-messages.util';

/**
 * Self-service profile. Each role updates through a different endpoint:
 * PATIENT → PUT /patients/update, DOCTOR/ASSISTANT → PUT /personal/update, ADMINISTRATOR → PUT /personal/update/{id}.
 * The JWT keeps the old name until the next login (no refresh endpoint).
 */
@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './profile-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class ProfilePageComponent {
  private readonly authApi = inject(AuthApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly isPatient = this.auth.hasRole('PATIENT');
  protected readonly clinic = this.auth.clinic;
  protected readonly resource = rxResource({ loader: () => this.authApi.me() });
  protected readonly profile = this.resource.value;
  protected readonly roleLabel = computed(() =>
    this.profile() ? ROLE_LABEL[this.profile()!.role] : '',
  );
  protected readonly errorMessage = computed(() =>
    apiErrorMessage(this.resource.error(), 'No se pudo cargar tu perfil'),
  );
  protected readonly avatar = computed(() => initials(this.profile()?.name));
  protected readonly saving = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: [''],
  });

  constructor() {
    effect(() => {
      const profile = this.profile();
      if (profile)
        this.form.reset({
          name: profile.name,
          email: profile.email,
          phoneNumber: profile.phoneNumber ?? '',
        });
    });
  }

  protected errorOf(name: keyof typeof this.form.controls): string | null {
    return fieldError(this.form.controls[name]);
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, phoneNumber } = this.form.getRawValue();
    const role = this.auth.role();
    let request: Observable<unknown>;
    if (role === 'PATIENT')
      request = this.patientsApi.updateSelf({
        name,
        email,
        phoneNumber: phoneNumber.trim() || null,
      });
    else if (role === 'ADMINISTRATOR')
      request = this.staffApi.update(this.auth.userId()!, { name, email });
    else request = this.staffApi.updateSelf({ name, email });

    this.saving.set(true);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.notifications.success('Perfil actualizado');
        this.resource.reload();
      },
      error: (error) =>
        this.notifications.error(apiErrorMessage(error, 'No se pudo actualizar el perfil')),
    });
  }
}
