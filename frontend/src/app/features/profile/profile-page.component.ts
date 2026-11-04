import { Dialog } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { filter, finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ROLES } from '../../core/auth/roles';
import { apiErrorMessage } from '../../core/http/api-error';
import { initials, rolesLabel } from '../../core/utils/labels.util';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../shared/ui/forms/form-field/form-field.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import {
  PHONE_ERRORS,
  currentPasswordWhenChanging,
  notBlank,
  phoneValidator,
} from '../../shared/utils/validation-messages.util';
import { DoctorModeCardComponent } from './components/doctor-mode-card/doctor-mode-card.component';
import { WhatsappQrComponent } from './components/whatsapp-qr/whatsapp-qr.component';
import { ProfileService } from './profile.service';

@Component({
  selector: 'app-profile-page',
  imports: [
    AlertComponent,
    DoctorModeCardComponent,
    SkeletonComponent,
    ReactiveFormsModule,
    FormFieldComponent,
    PageHeaderComponent,
    LoadErrorComponent,
  ],
  providers: [ProfileService],
  templateUrl: './profile-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePageComponent {
  private readonly dialog = inject(Dialog);
  protected readonly profile = inject(ProfileService);
  protected readonly auth = inject(AuthService);

  protected readonly me = this.profile.me;
  protected readonly roleLabel = computed(() => {
    const profile = this.me();
    if (!profile) return '';
    return rolesLabel(profile.alsoDoctor ? [profile.role, 'DOCTOR'] : [profile.role]);
  });
  protected readonly avatar = computed(() => initials(this.me()?.name));
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', notBlank],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', phoneValidator],
    currentPassword: ['', currentPasswordWhenChanging],
    newPassword: ['', Validators.minLength(8)],
  });

  constructor() {
    toObservable(this.me)
      .pipe(filter(Boolean), takeUntilDestroyed())
      .subscribe((profile) =>
        this.form.reset({
          name: profile.name,
          email: profile.email,
          phoneNumber: profile.phoneNumber ?? '',
        }),
      );
    this.form.controls.newPassword.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.currentPassword.updateValueAndValidity());
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.error.set(null));
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, phoneNumber, currentPassword, newPassword } = this.form.getRawValue();
    this.saving.set(true);
    this.profile
      .update({
        name: name.trim(),
        email: email.trim(),
        phoneNumber: phoneNumber.trim() || null,
        ...(newPassword ? { currentPassword, newPassword } : {}),
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo actualizar el perfil')),
      });
  }

  protected readonly canLinkWhatsapp = computed(() => this.auth.hasRole(...ROLES.admin));

  protected openWhatsapp(): void {
    this.dialog.open(WhatsappQrComponent, labelledByFrame());
  }
}
