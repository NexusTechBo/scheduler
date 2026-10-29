import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import {
  addDays,
  formatMonthShort,
  formatWeekdayShort,
  parseIso,
  toIso,
} from '../../../../core/utils/calendar.util';

const DATE_STRIP_DAYS = 14;

@Component({
  selector: 'app-date-strip',
  templateUrl: './date-strip.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateStripComponent {
  readonly selected = input.required<string>();
  readonly select = output<string>();

  private readonly nextDays = Array.from({ length: DATE_STRIP_DAYS }, (_, i) =>
    dayChip(addDays(new Date(), i)),
  );
  protected readonly days = computed(() => {
    const selected = this.selected();
    return this.nextDays.some((day) => day.iso === selected)
      ? this.nextDays
      : [...this.nextDays, dayChip(parseIso(selected))];
  });
}

function dayChip(date: Date) {
  return {
    iso: toIso(date),
    weekday: formatWeekdayShort(date),
    day: date.getDate(),
    month: formatMonthShort(date),
  };
}
