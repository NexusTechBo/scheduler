import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ROLES } from '../../core/navigation/navigation';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import { SegmentedTabsComponent, SegmentedTabItem } from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { AppointmentActionsService } from './appointment-actions.service';
import { AppointmentRow, AppointmentStatusFilter, AppointmentsResourceService } from './services/appointments-resource.service';

@Component({
  selector: 'app-appointments-page',
  imports: [
    RouterLink,
    PageHeaderComponent,
    SegmentedTabsComponent,
    StatusBadgeComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent
  ],
  providers: [AppointmentsResourceService],
  templateUrl: './appointments-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' }
})
export class AppointmentsPageComponent {
  private readonly auth = inject(AuthService);
  protected readonly actions = inject(AppointmentActionsService);
  protected readonly resource = inject(AppointmentsResourceService);

  protected readonly isPatient = this.auth.hasRole('PATIENT');
  protected readonly canBook = this.auth.hasRole(...ROLES.booking);
  protected readonly canManage = this.actions.canManage();

  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'ALL', label: 'Todas' },
    { id: 'PENDING', label: 'Pendientes' },
    { id: 'CONFIRMED', label: 'Confirmadas' },
    { id: 'CANCELLED', label: 'Canceladas' }
  ];

  protected setStatus(id: string): void {
    this.resource.setStatus(id as AppointmentStatusFilter);
  }

  protected open(row: AppointmentRow): void {
    this.actions.openDetail(row.id).subscribe((changed) => changed && this.resource.reloadAppointments());
  }

  protected confirm(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.actions.confirm(row.id, row.patientName).subscribe(() => this.resource.reloadAppointments());
  }

  protected cancel(row: AppointmentRow, event: Event): void {
    event.stopPropagation();
    this.actions.cancel(row.id, row.patientName).subscribe(() => this.resource.reloadAppointments());
  }
}
