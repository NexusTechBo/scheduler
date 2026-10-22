import { Injectable, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA } from '@angular/cdk/dialog';
import { rxResource } from '@angular/core/rxjs-interop';
import { finalize, map } from 'rxjs';
import { PatientsApi } from '../../../../../core/api/patients.api';
import { StaffApi } from '../../../../../core/api/staff.api';
import { apiErrorMessage } from '../../../../../core/http/api-error';
import { Patient } from '../../../../../core/models/api.models';
import { NotificationService } from '../../../../../shared/services/notification.service';

@Injectable()
export class AssignDoctorService {
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly notifications = inject(NotificationService);

  readonly patient = inject<{ patient: Patient }>(DIALOG_DATA).patient;

  private readonly assignedResource = rxResource({
    loader: () => this.patientsApi.doctors(this.patient.id),
  });
  private readonly activeDoctorsResource = rxResource({
    loader: () =>
      this.staffApi.doctors({ isActive: true, size: 200 }).pipe(map((page) => page.content)),
  });

  private readonly _assigning = signal(false);
  private readonly _message = signal('');
  readonly assigning = this._assigning.asReadonly();
  readonly message = this._message.asReadonly();
  readonly assignedDoctors = computed(() => this.assignedResource.value() ?? []);
  readonly assignedLoading = this.assignedResource.isLoading;
  readonly assignableDoctors = computed(() => {
    const assigned = new Set(this.assignedDoctors().map((doctor) => doctor.id));
    return (this.activeDoctorsResource.value() ?? []).filter((doctor) => !assigned.has(doctor.id));
  });

  assign(doctorId: number): void {
    this.changeAssignment(doctorId, true);
  }

  unassign(doctorId: number): void {
    this.changeAssignment(doctorId, false);
  }

  private changeAssignment(doctorId: number, assign: boolean): void {
    if (this.assigning()) return;
    const request = { patientId: this.patient.id, doctorId };
    const doctorName = [
      ...this.assignedDoctors(),
      ...(this.activeDoctorsResource.value() ?? []),
    ].find((doctor) => doctor.id === doctorId)?.name;
    this._assigning.set(true);
    (assign ? this.staffApi.assignPatient(request) : this.staffApi.removePatient(request))
      .pipe(finalize(() => this._assigning.set(false)))
      .subscribe({
        next: () => {
          this._message.set(
            `${doctorName ?? 'Doctor'} ${assign ? 'asignado' : 'quitado'} correctamente`,
          );
          this.assignedResource.reload();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error)),
      });
  }
}
