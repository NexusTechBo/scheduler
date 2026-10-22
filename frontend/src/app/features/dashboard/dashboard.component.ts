import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentListItemComponent } from '../../shared/ui/appointment-list-item/appointment-list-item.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { DashboardService } from './services/dashboard.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    AlertComponent,
    SkeletonComponent,
    RouterLink,
    PageHeaderComponent,
    AppointmentListItemComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [DashboardService],
  templateUrl: './dashboard.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  protected readonly dashboard = inject(DashboardService);
}
