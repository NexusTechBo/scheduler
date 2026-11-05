import { Dialog } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, Injector, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient } from '../../core/models/api.models';
import {
  PatientFormData,
  PatientFormDialogComponent,
} from '../../shared/components/patient-form-dialog/patient-form-dialog.component';
import { InitialsPipe } from '../../shared/pipes/initials.pipe';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { focusAfterLoad } from '../../shared/utils/focus-after-load.util';
import {
  AssignDoctorData,
  AssignDoctorDialogComponent,
} from './components/assign-doctor-dialog/assign-doctor-dialog.component';
import { PatientScope } from './patients.model';
import { PatientsService } from './patients.service';

@Component({
  selector: 'app-patients-page',
  imports: [
    InitialsPipe,
    LoadErrorComponent,
    SkeletonComponent,
    FormsModule,
    PageHeaderComponent,
    SegmentedTabsComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
    TranslatePipe,
  ],
  providers: [PatientsService],
  templateUrl: './patients-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PatientsPageComponent {
  private readonly i18n = inject(I18nService);
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  protected readonly patients = inject(PatientsService);
  private readonly focusRow = focusAfterLoad(this.patients.isLoading);

  protected setScope(scope: string): void {
    this.patients.setScope(scope as PatientScope);
  }

  protected openForm(patient?: Patient): void {
    this.dialog
      .open<Patient, PatientFormData>(PatientFormDialogComponent, {
        ...labelledByFrame(),
        data: { patient },
        injector: this.injector,
      })
      .closed.subscribe((saved) => {
        if (saved) this.patients.reload();
      });
  }

  protected manageDoctors(patient: Patient): void {
    this.dialog
      .open<boolean, AssignDoctorData>(AssignDoctorDialogComponent, {
        ...labelledByFrame(),
        data: { patient },
        disableClose: true,
      })
      .closed.subscribe((changed) => {
        if (changed && this.patients.ownListEnabled) this.patients.reloadMine();
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

  protected readonly tabs = computed<SegmentedTabItem[]>(() => {
    this.i18n.currentLang();
    return [
      { id: 'all', label: this.i18n.translate('patients.tabs.all') },
      { id: 'mine', label: this.i18n.translate('patients.tabs.mine') },
    ];
  });
}
