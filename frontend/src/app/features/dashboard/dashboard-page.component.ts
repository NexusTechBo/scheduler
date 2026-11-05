import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentListItemComponent } from '../../shared/components/appointment-list-item/appointment-list-item.component';
import { TimeRangePipe } from '../../shared/pipes/time-range.pipe';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { WeekChartComponent } from './components/week-chart/week-chart.component';
import { DashboardService } from './dashboard.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    WeekChartComponent,
    TimeRangePipe,
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
