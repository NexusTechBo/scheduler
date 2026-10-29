import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { Patient } from '../../../../core/models/api.models';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
} from '../../../../shared/utils/validation-messages.util';
import { PatientSaveService } from '../../../../shared/services/patient-save.service';
import { AlertComponent } from '../../../../shared/ui/alert/alert.component';

export interface PatientFormData {
  patient?: Patient;
}

@Component({
  selector: 'app-patient-form-dialog',
  imports: [AlertComponent, DialogFrameComponent, FormFieldComponent, ReactiveFormsModule],
  templateUrl: './patient-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientFormDialogComponent {
  private readonly patients = inject(PatientSaveService);
  protected readonly ref = inject<DialogRef<Patient>>(DialogRef);
  protected readonly patient = inject<PatientFormData>(DIALOG_DATA).patient;

  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: [this.patient?.name ?? '', notBlank],
    phoneNumber: [this.patient?.phoneNumber ?? '', [notBlank, phoneValidator]],
    email: [this.patient?.email ?? '', Validators.email],
    ci: [this.patient?.ci ?? '', [notBlank, ciValidator]],
  });

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, email, phoneNumber, ci } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.patients
      .save(this.patient, {
        name: name.trim(),
        ci: ci.trim(),
        phoneNumber: phoneNumber.trim(),
        email: email.trim() || null,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => this.ref.close(saved),
        error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo guardar el paciente')),
      });
  }
}
