import { Component, computed, input, output } from '@angular/core';
import { AppointmentStatus, AppointmentSummaryItem } from '../../../core/models/api.models';
import { CalendarDay, formatLongDate, toIso } from '../../../core/utils/calendar.util';
import { STATUS_STYLE } from '../../../core/utils/labels.util';

const STATUS_ORDER: AppointmentStatus[] = ['PENDING', 'CONFIRMED', 'CANCELLED'];

@Component({
  selector: 'app-calendar-day-cell',
  templateUrl: './calendar-day-cell.component.html',
  host: { class: 'block' },
})
export class CalendarDayCellComponent {
  day = input.required<CalendarDay>();
  appointments = input<AppointmentSummaryItem[]>([]);
  selected = input(false);

  select = output<string>();

  protected readonly today = toIso(new Date());

  /** One dot per status present that day. */
  protected readonly dotTones = computed(() => {
    const present = new Set(this.appointments().map((appointment) => appointment.status));
    return STATUS_ORDER.filter((status) => present.has(status)).map(
      (status) => STATUS_STYLE[status].dot,
    );
  });
  protected readonly activeCount = computed(
    () => this.appointments().filter((appointment) => appointment.status !== 'CANCELLED').length,
  );
  protected readonly ariaLabel = computed(
    () => `${formatLongDate(this.day().iso)}, ${this.activeCount()} citas`,
  );
}
