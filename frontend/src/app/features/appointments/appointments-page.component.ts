import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { APPOINTMENT_STATUSES } from '../../core/utils/labels.util';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
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
import { AppointmentRow, AppointmentStatusFilter } from './appointments.model';
import { AppointmentsService } from './appointments.service';
import { GuestLinkButtonComponent } from './components/guest-link-button/guest-link-button.component';

const STATUS_I18N_KEYS: Record<string, string> = {
  PENDING: 'appointments.tabs.pending',
  CONFIRMED: 'appointments.tabs.confirmed',
  CANCELLED: 'appointments.tabs.cancelled',
  COMPLETED: 'appointments.tabs.completed',
};

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
    TranslatePipe,
  ],
  providers: [AppointmentsService],
  templateUrl: './appointments-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentsPageComponent {
  private readonly i18n = inject(I18nService);
  private readonly ownDataOnly = inject(AuthService).restrictedToOwnData();
  protected readonly appointments = inject(AppointmentsService);
  private readonly focusRow = focusAfterLoad(this.appointments.isLoading);

  protected readonly showDoctor = !this.ownDataOnly;

  protected readonly subtitle = computed(() => {
    this.i18n.currentLang();
    const key = this.ownDataOnly ? 'appointments.subtitles.own' : 'appointments.subtitles.all';
    return this.i18n.translate(key);
  });

  protected readonly tabs = computed<SegmentedTabItem[]>(() => {
    this.i18n.currentLang();
    return [
      { id: 'ALL', label: this.i18n.translate('appointments.tabs.all') },
      ...APPOINTMENT_STATUSES.map((status) => ({
        id: status,
        label: this.i18n.translate(STATUS_I18N_KEYS[status] ?? status),
      })),
    ];
  });

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
