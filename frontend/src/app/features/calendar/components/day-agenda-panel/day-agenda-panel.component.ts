import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { AppointmentSummaryItem } from '../../../../core/models/api.models';
import { AppointmentListItemComponent } from '../../../../shared/components/appointment-list-item/appointment-list-item.component';
import { LoadErrorComponent } from '../../../../shared/ui/load-error/load-error.component';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-day-agenda-panel',
  imports: [AppointmentListItemComponent, LoadErrorComponent, SkeletonComponent, UiIconComponent],
  templateUrl: './day-agenda-panel.component.html',
  host: {
    class: 'glass-panel flex min-h-72 w-full shrink-0 flex-col rounded-3xl p-6 xl:w-[22rem]',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayAgendaPanelComponent {
  dayLabel = input.required<string>();
  appointments = input.required<AppointmentSummaryItem[] | null>();
  canCreate = input(true);
  errorMessage = input<string | null>(null);

  newAppointment = output<void>();
  open = output<AppointmentSummaryItem>();
  retry = output<void>();
}
