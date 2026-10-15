import { Component, computed, effect, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize } from 'rxjs';
import { CatalogsApi } from '../../core/api/catalogs.api';
import { StaffApi } from '../../core/api/staff.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { ROLE_ID, Staff } from '../../core/models/api.models';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { fieldError } from '../../shared/utils/validation-messages.util';

export interface StaffFormData {
  /** Absent → create. */
  staff?: Staff;
}

/** Only DOCTOR and ASSISTANT can be created (backend @ValidRole); doctors need a specialty, assistants must not have one. */
@Component({
  selector: 'app-staff-form-dialog',
  imports: [DialogFrameComponent, ReactiveFormsModule],
  templateUrl: './staff-form-dialog.component.html',
})
export class StaffFormDialogComponent {
  private readonly staffApi = inject(StaffApi);
  private readonly catalogsApi = inject(CatalogsApi);
  private readonly ref = inject<DialogRef<Staff>>(DialogRef);
  protected readonly data = inject<StaffFormData>(DIALOG_DATA);

  protected readonly isEdit = !!this.data.staff;
  protected readonly roles = [
    { id: ROLE_ID.DOCTOR, label: 'Doctor' },
    { id: ROLE_ID.ASSISTANT, label: 'Asistente' },
  ];
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly specialtiesResource = rxResource({
    loader: () => this.catalogsApi.specialties(),
  });

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: [this.data.staff?.name ?? '', Validators.required],
    email: [this.data.staff?.email ?? '', [Validators.required, Validators.email]],
    ci: ['', this.isEdit ? [] : [Validators.required, Validators.pattern(/^\d+$/)]],
    password: ['', this.isEdit ? [] : [Validators.required, Validators.minLength(8)]],
    roleId: [this.data.staff?.roleName === 'ASSISTANT' ? ROLE_ID.ASSISTANT : ROLE_ID.DOCTOR],
    specialtyId: [null as number | null],
  });

  private readonly roleId = toSignal(this.form.controls.roleId.valueChanges, {
    initialValue: this.form.controls.roleId.value,
  });
  protected readonly needsSpecialty = computed(
    () => this.roleId() === ROLE_ID.DOCTOR && this.data.staff?.roleName !== 'ADMINISTRATOR',
  );

  constructor() {
    // PersonalResponse only carries the specialty name: preselect its id once the catalog arrives.
    const current = this.data.staff?.specialtyName;
    effect(() => {
      const match = (this.specialtiesResource.value() ?? []).find(
        (specialty) => specialty.name === current,
      );
      if (match && this.form.controls.specialtyId.value === null)
        this.form.controls.specialtyId.setValue(match.id);
    });
  }

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
    if (this.needsSpecialty() && !value.specialtyId) {
      this.error.set('Selecciona la especialidad del doctor');
      return;
    }
    const specialtyId = this.needsSpecialty() ? value.specialtyId : null;

    const request: Observable<Staff> = this.isEdit
      ? this.staffApi.update(this.data.staff!.id, {
          name: value.name,
          email: value.email,
          specialtyId,
        })
      : this.staffApi.create({
          name: value.name,
          email: value.email,
          ci: value.ci,
          password: value.password,
          roleId: value.roleId,
          specialtyId,
        });

    this.saving.set(true);
    this.error.set(null);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (staff) => this.ref.close(staff),
      error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo guardar')),
    });
  }
}
