import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { AppointmentSummaryItem } from '../../../../core/models/api.models';
import { CalendarDay, formatLongDate, toIso } from '../../../../core/utils/calendar.util';
import { APPOINTMENT_STATUSES, STATUS_STYLE } from '../../../../core/utils/labels.util';

@Component({
  selector: 'app-calendar-day-cell',
  templateUrl: './calendar-day-cell.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarDayCellComponent {
  day = input.required<CalendarDay>();
  appointments = input<AppointmentSummaryItem[] | null>(null);
  selected = input(false);

  select = output<string>();

  protected readonly today = toIso(new Date());

  protected readonly dotTones = computed(() => {
    const present = new Set((this.appointments() ?? []).map((appointment) => appointment.status));
    return APPOINTMENT_STATUSES.filter((status) => present.has(status)).map(
      (status) => STATUS_STYLE[status].dot,
    );
  });
  protected readonly count = computed(() => this.appointments()?.length ?? null);
  protected readonly ariaLabel = computed(() => {
    const date = formatLongDate(this.day().iso);
    const count = this.count();
    return count === null ? date : `${date}, ${count} ${count === 1 ? 'cita' : 'citas'}`;
  });
}
