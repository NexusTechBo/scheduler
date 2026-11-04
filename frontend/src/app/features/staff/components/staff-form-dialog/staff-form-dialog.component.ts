import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize, startWith } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Role, Staff } from '../../../../core/models/api.models';
import { ROLE_LABEL } from '../../../../core/utils/labels.util';
import { AlertComponent } from '../../../../shared/ui/alert/alert.component';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/forms/select/select.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  USERNAME_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
  specialtyForDoctor,
  usernameValidator,
} from '../../../../shared/utils/validation-messages.util';
import { StaffService } from '../../staff.service';

const EDITABLE_ROLES: Role[] = ['DOCTOR', 'ASSISTANT'];

export interface StaffFormData {
  staff?: Staff;
}

function initialRole(staff: Staff | undefined): Role {
  if (!staff) return 'DOCTOR';
  return staff.alsoDoctor ? 'DOCTOR' : staff.role;
}

@Component({
  selector: 'app-staff-form-dialog',
  imports: [
    AlertComponent,
    DialogFrameComponent,
    FormFieldComponent,
    ReactiveFormsModule,
    SelectComponent,
  ],
  templateUrl: './staff-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffFormDialogComponent {
  private readonly staffService = inject(StaffService);
  protected readonly ref = inject(DialogRef);
  protected readonly staff = inject<StaffFormData>(DIALOG_DATA).staff;

  private readonly clinic = inject(AuthService).clinic;
  protected readonly clinicSuffix = computed(() => {
    const clinic = this.clinic();
    return clinic ? `@${clinic.slug}.com` : '';
  });
  protected readonly canChangeRole = this.staff?.role !== 'ADMINISTRATOR';
  protected readonly roles = EDITABLE_ROLES.map((role) => ({ id: role, label: ROLE_LABEL[role] }));
  protected readonly usernameErrors = USERNAME_ERRORS;
  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    role: [initialRole(this.staff)],
    name: [this.staff?.name ?? '', notBlank],
    email: [this.staff?.email ?? '', [Validators.required, Validators.email]],
    phoneNumber: [this.staff?.phoneNumber ?? '', phoneValidator],
    ci: ['', this.staff ? [] : [notBlank, ciValidator]],
    username: ['', this.staff ? [] : [Validators.required, usernameValidator]],
    password: [
      '',
      this.staff ? [Validators.minLength(8)] : [Validators.required, Validators.minLength(8)],
    ],
    specialtyId: [this.staff?.specialtyId ?? null, specialtyForDoctor],
  });

  private readonly role = toSignal(this.form.controls.role.valueChanges, {
    initialValue: this.form.controls.role.value,
  });
  protected readonly isDoctor = computed(() => this.role() === 'DOCTOR');
  protected readonly specialtyOptions = computed<SelectOption<number | null>[]>(() =>
    this.staffService.specialties().map((s) => ({ value: s.id, label: s.name })),
  );

  constructor() {
    this.form.controls.role.valueChanges
      .pipe(startWith(null), takeUntilDestroyed())
      .subscribe(() => this.form.controls.specialtyId.updateValueAndValidity());
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.error.set(null));
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const common = {
      name: value.name.trim(),
      email: value.email.trim(),
      phoneNumber: value.phoneNumber.trim() || null,
      role: value.role,
      specialtyId: value.role === 'DOCTOR' ? value.specialtyId : null,
    };
    const request: Observable<Staff> = this.staff
      ? this.staffService.update(this.staff.id, { ...common, password: value.password || null })
      : this.staffService.create({
          ...common,
          ci: value.ci.trim(),
          username: value.username,
          password: value.password,
        });

    this.saving.set(true);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => this.ref.close(),
      error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo guardar')),
    });
  }
}
