import { Component, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { map } from 'rxjs';
import { PatientsApi } from '../../core/api/patients.api';
import { StaffApi } from '../../core/api/staff.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { Patient } from '../../core/models/api.models';
import { initials } from '../../core/utils/labels.util';
import { NotificationService } from '../../shared/services/notification.service';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';

export interface AssignDoctorData {
  patient: Patient;
}

/** ASSISTANT: manage which doctors treat a patient (doctor_patient relation). */
@Component({
  selector: 'app-assign-doctor-dialog',
  imports: [DialogFrameComponent, FormsModule, UiIconComponent],
  templateUrl: './assign-doctor-dialog.component.html'
})
export class AssignDoctorDialogComponent {
  private readonly staffApi = inject(StaffApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly notifications = inject(NotificationService);
  private readonly ref = inject<DialogRef<void>>(DialogRef);
  protected readonly data = inject<AssignDoctorData>(DIALOG_DATA);

  protected readonly busy = signal(false);
  protected readonly doctorToAdd = signal<number | null>(null);
  protected readonly initials = initials;

  protected readonly assignedResource = rxResource({ loader: () => this.patientsApi.doctors(this.data.patient.id) });
  private readonly doctorsResource = rxResource({
    loader: () => this.staffApi.doctors({ isActive: true, size: 200 }).pipe(map((page) => page.content))
  });

  protected readonly available = computed(() => {
    const assigned = new Set((this.assignedResource.value() ?? []).map((doctor) => doctor.id));
    return (this.doctorsResource.value() ?? []).filter((doctor) => !assigned.has(doctor.id));
  });

  protected add(): void {
    const doctorId = this.doctorToAdd();
    if (!doctorId) return;
    this.busy.set(true);
    this.staffApi.assignPatient({ patientId: this.data.patient.id, doctorId }).subscribe({
      next: () => {
        this.doctorToAdd.set(null);
        this.assignedResource.reload();
        this.busy.set(false);
      },
      error: (error) => {
        this.notifications.error(apiErrorMessage(error));
        this.busy.set(false);
      }
    });
  }

  protected remove(doctorId: number): void {
    this.busy.set(true);
    this.staffApi.removePatient({ patientId: this.data.patient.id, doctorId }).subscribe({
      next: () => {
        this.assignedResource.reload();
        this.busy.set(false);
      },
      error: (error) => {
        this.notifications.error(apiErrorMessage(error));
        this.busy.set(false);
      }
    });
  }

  protected close(): void {
    this.ref.close();
  }
}
