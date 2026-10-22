import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import {
  canBookAppointments,
  canCancelAppointment,
  canConfirmAppointment,
} from '../../core/auth/permissions';
import { AppointmentStatus } from '../../core/models/api.models';
import { APPOINTMENT_STATUSES, STATUS_PLURAL_LABEL } from '../../core/utils/labels.util';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import {
  AppointmentRow,
  AppointmentStatusFilter,
  AppointmentsPageService,
} from './services/appointments-page.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';
import { focusAfterLoad } from '../../shared/ui/focus-after-load';

@Component({
  selector: 'app-appointments-page',
  imports: [
    AlertComponent,
    SpecialtyLabelPipe,
    SkeletonComponent,
    RouterLink,
    PageHeaderComponent,
    SegmentedTabsComponent,
    StatusBadgeComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [AppointmentsPageService],
  templateUrl: './appointments-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentsPageComponent {
  private readonly role = inject(AuthService).role;
  protected readonly page = inject(AppointmentsPageService);
  private readonly focusRow = focusAfterLoad(this.page.isLoading);

  protected readonly canBook = canBookAppointments(this.role());
  protected readonly showDoctor = this.role() !== 'DOCTOR';
  protected readonly subtitle =
    this.role() === 'DOCTOR' ? 'Tus citas' : 'Todas las citas de la clínica';
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'ALL', label: 'Todas' },
    ...APPOINTMENT_STATUSES.map((status) => ({ id: status, label: STATUS_PLURAL_LABEL[status] })),
  ];

  protected canConfirm(status: AppointmentStatus): boolean {
    return canConfirmAppointment(this.role(), status);
  }

  protected canCancel(status: AppointmentStatus): boolean {
    return canCancelAppointment(this.role(), status);
  }

  protected setStatus(id: string): void {
    this.page.setStatus(id as AppointmentStatusFilter);
  }

  protected open(row: AppointmentRow, event: MouseEvent): void {
    if (event.detail <= 1) this.page.open(row);
  }

  protected confirm(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.page.confirm(row).subscribe(() => this.refocus(row));
  }

  protected cancel(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.page.cancel(row).subscribe(() => this.refocus(row));
  }

  private refocus(row: AppointmentRow): void {
    this.focusRow(`#appointment-${row.id}`, 'tbody button');
  }
}
