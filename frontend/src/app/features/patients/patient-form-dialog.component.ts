import { Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { PatientsApi } from '../../core/api/patients.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { Patient } from '../../core/models/api.models';
import { fieldError } from '../../shared/utils/validation-messages.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';

export interface PatientFormData {
  /** Absent → create. */
  patient?: Patient;
}

@Component({
  selector: 'app-patient-form-dialog',
  imports: [DialogFrameComponent, ReactiveFormsModule],
  templateUrl: './patient-form-dialog.component.html',
})
export class PatientFormDialogComponent {
  private readonly api = inject(PatientsApi);
  private readonly ref = inject<DialogRef<Patient>>(DialogRef);
  protected readonly data = inject<PatientFormData>(DIALOG_DATA);

  protected readonly isEdit = !!this.data.patient;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Mirrors PatientRegisterRequest / PatientRequest validation. CI and password only exist on create. */
  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: [this.data.patient?.name ?? '', Validators.required],
    email: [this.data.patient?.email ?? '', [Validators.required, Validators.email]],
    phoneNumber: [this.data.patient?.phoneNumber ?? ''],
    ci: ['', this.isEdit ? [] : [Validators.required]],
    password: ['', this.isEdit ? [] : [Validators.required, Validators.minLength(8)]],
  });

  protected errorOf(name: keyof typeof this.form.controls): string | null {
    return fieldError(this.form.controls[name]);
  }

  protected close(): void {
    this.ref.close();
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const phoneNumber = value.phoneNumber.trim() || undefined;
    const request: Observable<Patient> = this.isEdit
      ? this.api.update(this.data.patient!.id, {
          name: value.name,
          email: value.email,
          phoneNumber: phoneNumber ?? null,
        })
      : this.api.create({
          name: value.name,
          email: value.email,
          ci: value.ci,
          password: value.password,
          phoneNumber,
        });

    this.saving.set(true);
    this.error.set(null);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (patient) => this.ref.close(patient),
      error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo guardar el paciente')),
    });
  }
}
