import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentListItemComponent } from '../../shared/appointments/components/appointment-list-item/appointment-list-item.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { DashboardService } from './dashboard.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    LoadErrorComponent,
    SkeletonComponent,
    RouterLink,
    PageHeaderComponent,
    AppointmentListItemComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [DashboardService],
  templateUrl: './dashboard-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPageComponent {
  protected readonly dashboard = inject(DashboardService);
}
