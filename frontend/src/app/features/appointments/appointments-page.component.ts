import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { canCancelAppointment, canConfirmAppointment } from '../../core/auth/permissions';
import { APPOINTMENT_STATUSES, STATUS_PLURAL_LABEL } from '../../core/utils/labels.util';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { focusAfterLoad } from '../../shared/utils/focus-after-load.util';
import {
  AppointmentRow,
  AppointmentStatusFilter,
  AppointmentsService,
} from './appointments.service';
import { GuestLinkButtonComponent } from './components/guest-link-button/guest-link-button.component';

@Component({
  selector: 'app-appointments-page',
  imports: [
    LowerCasePipe,
    LoadErrorComponent,
    SpecialtyLabelPipe,
    SkeletonComponent,
    RouterLink,
    PageHeaderComponent,
    SegmentedTabsComponent,
    StatusBadgeComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
    GuestLinkButtonComponent,
  ],
  providers: [AppointmentsService],
  templateUrl: './appointments-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentsPageComponent {
  private readonly isDoctor = inject(AuthService).hasRole('DOCTOR');
  protected readonly appointments = inject(AppointmentsService);
  private readonly focusRow = focusAfterLoad(this.appointments.isLoading);

  protected readonly canConfirm = canConfirmAppointment;
  protected readonly canCancel = canCancelAppointment;
  protected readonly showDoctor = !this.isDoctor;
  protected readonly subtitle = this.isDoctor ? 'Tus citas' : 'Todas las citas de la clínica';
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'ALL', label: 'Todas' },
    ...APPOINTMENT_STATUSES.map((status) => ({ id: status, label: STATUS_PLURAL_LABEL[status] })),
  ];

  protected setStatus(id: string): void {
    this.appointments.setStatus(id as AppointmentStatusFilter);
  }

  protected open(row: AppointmentRow, event: MouseEvent): void {
    if (event.detail <= 1) this.appointments.open(row);
  }

  protected confirm(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.appointments.confirm(row).subscribe(() => this.refocus(row));
  }

  protected cancel(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.appointments.cancel(row).subscribe(() => this.refocus(row));
  }

  private refocus(row: AppointmentRow): void {
    this.focusRow(`#appointment-${row.id}`, 'tbody button');
  }
}
