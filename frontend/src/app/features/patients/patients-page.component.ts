import { ChangeDetectionStrategy, Component, Injector, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FormsModule } from '@angular/forms';
import { Patient } from '../../core/models/api.models';
import { initials } from '../../core/utils/labels.util';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import {
  PatientFormData,
  PatientFormDialogComponent,
} from './components/patient-form-dialog/patient-form-dialog.component';
import {
  AssignDoctorData,
  AssignDoctorDialogComponent,
} from './components/assign-doctor-dialog/assign-doctor-dialog.component';
import { PatientScope, PatientsService } from './patients.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { focusAfterLoad } from '../../shared/utils/focus-after-load.util';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';

@Component({
  selector: 'app-patients-page',
  imports: [
    LoadErrorComponent,
    SkeletonComponent,
    FormsModule,
    PageHeaderComponent,
    SegmentedTabsComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [PatientsService],
  templateUrl: './patients-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientsPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  protected readonly patients = inject(PatientsService);
  private readonly focusRow = focusAfterLoad(this.patients.isLoading);

  protected readonly initials = initials;
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'mine', label: 'Mis pacientes' },
    { id: 'all', label: 'Todos' },
  ];

  protected setScope(scope: string): void {
    this.patients.setScope(scope as PatientScope);
  }

  protected openForm(patient?: Patient): void {
    this.dialog.open<void, PatientFormData>(PatientFormDialogComponent, {
      ...labelledByFrame(),
      data: { patient },
      injector: this.injector,
    });
  }

  protected manageDoctors(patient: Patient): void {
    this.dialog.open<void, AssignDoctorData>(AssignDoctorDialogComponent, {
      ...labelledByFrame(),
      data: { patient },
    });
  }

  protected deactivate(patient: Patient): void {
    this.patients
      .deactivate(patient)
      .subscribe(() =>
        this.focusRow(
          `#patient-status-${patient.id}`,
          `#patient-edit-${patient.id}`,
          'tbody button',
        ),
      );
  }

  protected activate(patient: Patient): void {
    this.patients
      .activate(patient)
      .subscribe(() => this.focusRow(`#patient-status-${patient.id}`, 'tbody button'));
  }

  protected toggleMine(patient: Patient): void {
    const list = this.patients.patients();
    const next = list[list.indexOf(patient) + 1];
    this.patients
      .toggleMine(patient)
      .subscribe(() =>
        this.focusRow(
          `#patient-toggle-${patient.id}`,
          ...(next ? [`#patient-toggle-${next.id}`] : []),
          'tbody button',
        ),
      );
  }
}
