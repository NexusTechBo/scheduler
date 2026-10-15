import { Component, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { FormsModule } from '@angular/forms';
import { filter, map, of, switchMap } from 'rxjs';
import { PatientsApi } from '../../core/api/patients.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { Patient } from '../../core/models/api.models';
import { initials } from '../../core/utils/labels.util';
import { NotificationService } from '../../shared/services/notification.service';
import { ConfirmService } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { AssignDoctorData, AssignDoctorDialogComponent } from './assign-doctor-dialog.component';
import { PatientFormData, PatientFormDialogComponent } from './patient-form-dialog.component';
import { PatientScope, PatientsResourceService } from './services/patients-resource.service';

@Component({
  selector: 'app-patients-page',
  imports: [
    FormsModule,
    PageHeaderComponent,
    SegmentedTabsComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [PatientsResourceService],
  templateUrl: './patients-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class PatientsPageComponent {
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(Dialog);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  protected readonly resource = inject(PatientsResourceService);

  protected readonly isDoctor = this.resource.isDoctor;
  protected readonly initials = initials;
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'mine', label: 'Mis pacientes' },
    { id: 'all', label: 'Todos' },
  ];

  protected setScope(scope: string): void {
    this.resource.setScope(scope as PatientScope);
  }

  protected openForm(patient?: Patient): void {
    this.dialog
      .open<Patient, PatientFormData>(PatientFormDialogComponent, {
        data: { patient },
        backdropClass: 'glass-backdrop',
      })
      .closed.pipe(
        filter(Boolean),
        // New patients of a doctor are linked to them right away so they show up in "Mis pacientes".
        switchMap((saved) =>
          !patient && this.isDoctor
            ? this.staffApi
                .assignPatient({ patientId: saved.id, doctorId: this.auth.userId()! })
                .pipe(map(() => saved))
            : of(saved),
        ),
      )
      .subscribe({
        next: () => {
          this.notifications.success(patient ? 'Paciente actualizado' : 'Paciente creado');
          this.resource.reloadPatients();
        },
        error: (error) => {
          // Only assignPatient can fail here: the dialog already created the patient.
          this.notifications.error(
            'Paciente creado, pero no se pudo asignar: ' + apiErrorMessage(error),
          );
          this.resource.reloadPatients();
        },
      });
  }

  protected manageDoctors(patient: Patient): void {
    this.dialog
      .open<void, AssignDoctorData>(AssignDoctorDialogComponent, {
        data: { patient },
        backdropClass: 'glass-backdrop',
      })
      .closed.subscribe(() => this.resource.reloadPatients());
  }

  protected toggleMine(patient: Patient): void {
    const request = { patientId: patient.id, doctorId: this.auth.userId()! };
    const mine = this.resource.isMine(patient);
    (mine ? this.staffApi.removePatient(request) : this.staffApi.assignPatient(request)).subscribe({
      next: () => {
        this.notifications.success(
          mine ? 'Paciente quitado de tu lista' : 'Paciente agregado a tu lista',
        );
        this.resource.reloadPatients();
      },
      error: (error) => this.notifications.error(apiErrorMessage(error)),
    });
  }

  protected deactivate(patient: Patient): void {
    this.confirm
      .ask({
        title: 'Desactivar paciente',
        message: `${patient.name} ya no podrá agendar nuevas citas.`,
        confirmLabel: 'Desactivar',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.patientsApi.deactivate(patient.id)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Paciente desactivado');
          this.resource.reloadPatients();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error)),
      });
  }
}
