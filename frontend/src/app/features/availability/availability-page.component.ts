import { ChangeDetectionStrategy, Component, Injector, inject } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { Dialog } from '@angular/cdk/dialog';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FormsModule } from '@angular/forms';
import { formatLongDate } from '../../core/utils/calendar.util';
import { TimeRangePipe } from '../../shared/pipes/time-range.pipe';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { DatePickerComponent } from '../../shared/ui/forms/date-picker/date-picker.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { AvailabilityFormDialogComponent } from './components/availability-form-dialog/availability-form-dialog.component';
import { AvailabilityService } from './services/availability.service';
import { ExceptionFormDialogComponent } from './components/exception-form-dialog/exception-form-dialog.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';

@Component({
  selector: 'app-availability-page',
  imports: [
    LowerCasePipe,
    TimeRangePipe,
    LoadErrorComponent,
    SkeletonComponent,
    FormsModule,
    DatePickerComponent,
    PageHeaderComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [AvailabilityService],
  templateUrl: './availability-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvailabilityPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  protected readonly availability = inject(AvailabilityService);

  protected readonly formatLongDate = formatLongDate;

  protected addAvailability(): void {
    this.dialog.open(AvailabilityFormDialogComponent, {
      ...labelledByFrame(),
      injector: this.injector,
    });
  }

  protected addException(): void {
    this.dialog.open(ExceptionFormDialogComponent, {
      ...labelledByFrame(),
      injector: this.injector,
    });
  }
}
