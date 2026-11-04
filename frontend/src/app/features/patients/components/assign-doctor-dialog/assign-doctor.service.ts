import { DIALOG_DATA } from '@angular/cdk/dialog';
import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { PatientsApi } from '../../../../core/api/patients.api';
import { StaffApi } from '../../../../core/api/staff.api';
import { DoctorsStore } from '../../../../core/catalogs/doctors.store';
import { apiErrorMessage, resourceError } from '../../../../core/http/api-error';
import { NotificationService } from '../../../../shared/services/notification.service';
import type { AssignDoctorData } from './assign-doctor-dialog.component';

@Injectable()
export class AssignDoctorService {
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly doctorsStore = inject(DoctorsStore);
  private readonly notifications = inject(NotificationService);

  readonly patient = inject<AssignDoctorData>(DIALOG_DATA).patient;

  private readonly assignedResource = rxResource({
    stream: () => this.patientsApi.doctors(this.patient.id),
  });

  private readonly _assigning = signal(false);
  private readonly _message = signal('');
  private readonly _changed = signal(false);
  readonly assigning = this._assigning.asReadonly();
  readonly message = this._message.asReadonly();
  readonly changed = this._changed.asReadonly();
  readonly assignedDoctors = computed(() =>
    this.assignedResource.hasValue() ? this.assignedResource.value() : [],
  );
  readonly assignedLoading = this.assignedResource.isLoading;
  readonly assignedErrorMessage = resourceError(
    this.assignedResource,
    'No se pudieron cargar los doctores asignados',
  );
  readonly doctorsErrorMessage = this.doctorsStore.errorMessage;
  readonly assignableDoctors = computed(() => {
    const assigned = new Set(this.assignedDoctors().map((doctor) => doctor.id));
    return this.doctorsStore.doctors().filter((doctor) => !assigned.has(doctor.id));
  });

  constructor() {
    this.doctorsStore.load();
  }

  reloadAssigned(): void {
    this.assignedResource.reload();
  }

  reloadDoctors(): void {
    this.doctorsStore.reload();
  }

  assign(doctorId: number): void {
    this.changeAssignment(doctorId, true);
  }

  unassign(doctorId: number): void {
    this.changeAssignment(doctorId, false);
  }

  private changeAssignment(doctorId: number, assign: boolean): void {
    if (this.assigning()) return;
    const request = { patientId: this.patient.id, doctorId };
    const doctorName = [...this.assignedDoctors(), ...this.doctorsStore.doctors()].find(
      (doctor) => doctor.id === doctorId,
    )?.name;
    this._assigning.set(true);
    (assign ? this.staffApi.assignPatient(request) : this.staffApi.removePatient(request))
      .pipe(finalize(() => this._assigning.set(false)))
      .subscribe({
        next: () => {
          this._message.set(
            `${doctorName ?? 'Doctor'} ${assign ? 'asignado' : 'quitado'} correctamente`,
          );
          this._changed.set(true);
          this.assignedResource.reload();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error)),
      });
  }
}
