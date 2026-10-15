import { Component, input, output } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { AppointmentListItemComponent } from '../appointment-list-item/appointment-list-item.component';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-day-agenda-panel',
  imports: [AppointmentListItemComponent, UiIconComponent],
  templateUrl: './day-agenda-panel.component.html',
  host: {
    class: 'glass-panel flex min-h-72 w-full shrink-0 flex-col rounded-3xl p-6 xl:w-[22rem]',
  },
})
export class DayAgendaPanelComponent {
  dayLabel = input.required<string>();
  appointments = input.required<AppointmentSummaryItem[]>();
  /** Hides the "new appointment" button (roles that cannot book, or past days). */
  canCreate = input(true);
  doctorAsTitle = input(false);

  newAppointment = output<void>();
  open = output<AppointmentSummaryItem>();
}
