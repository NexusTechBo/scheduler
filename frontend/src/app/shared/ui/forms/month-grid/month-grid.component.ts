import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';
import {
  CalendarDay,
  addDays,
  buildMonthMatrix,
  formatLongDate,
  formatMonthLabel,
  parseIso,
  startOfWeek,
  toIso,
} from '../../../../core/utils/calendar.util';
import { DAYS_OF_WEEK } from '../../../../core/utils/labels.util';
import { UiIconComponent } from '../../ui-icon/ui-icon.component';

const ARROW_DAYS: Record<string, number> = {
  ArrowLeft: -1,
  ArrowRight: 1,
  ArrowUp: -7,
  ArrowDown: 7,
};

@Component({
  selector: 'app-month-grid',
  imports: [UiIconComponent],
  templateUrl: './month-grid.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthGridComponent {
  readonly month = model.required<Date>();
  readonly start = input<string | null>(null);
  readonly end = input<string | null>(null);
  readonly min = input<string | null>(null);

  readonly pick = output<string>();
  readonly hover = output<string | null>();

  protected readonly weekdays = DAYS_OF_WEEK;
  protected readonly formatLongDate = formatLongDate;
  private readonly today = toIso(new Date());
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly active = signal<string | null>(null);

  protected readonly focusIso = computed(() => {
    const monthKey = toIso(this.month()).slice(0, 7);
    const candidates = [this.active(), this.start(), this.today];
    return candidates.find((iso) => iso?.startsWith(monthKey)) ?? `${monthKey}-01`;
  });

  protected readonly monthLabel = computed(() => formatMonthLabel(this.month()));
  protected readonly weeks = computed(() =>
    buildMonthMatrix(this.month().getFullYear(), this.month().getMonth()),
  );

  protected shiftMonth(delta: number): void {
    this.month.update((date) => new Date(date.getFullYear(), date.getMonth() + delta, 1));
  }

  protected onKeydown(event: KeyboardEvent, iso: string): void {
    const target = keyTarget(event, parseIso(iso));
    if (!target) return;
    event.preventDefault();
    const next = toIso(target);
    if (this.outOfBounds(next)) return;
    this.active.set(next);
    if (!next.startsWith(toIso(this.month()).slice(0, 7)))
      this.month.set(new Date(target.getFullYear(), target.getMonth(), 1));
    afterNextRender(
      () => this.host.nativeElement.querySelector<HTMLElement>(`[data-iso="${next}"]`)?.focus(),
      { injector: this.injector },
    );
  }

  private outOfBounds(iso: string): boolean {
    const min = this.min();
    return !!min && iso < min;
  }

  protected dayState(day: CalendarDay) {
    const start = this.start();
    const end = this.end();
    const [lo, hi] = start && end ? [start, end].sort() : [null, null];
    return {
      edge: day.iso === start || day.iso === end,
      inRange: !!lo && !!hi && day.iso > lo && day.iso < hi,
      isToday: day.iso === this.today,
      disabled: this.outOfBounds(day.iso),
    };
  }
}

function keyTarget(event: KeyboardEvent, date: Date): Date | null {
  if (ARROW_DAYS[event.key]) return addDays(date, ARROW_DAYS[event.key]);
  if (event.key === 'Home') return startOfWeek(date);
  if (event.key === 'End') return addDays(startOfWeek(date), 6);
  if (event.key !== 'PageUp' && event.key !== 'PageDown') return null;
  const months = (event.key === 'PageUp' ? -1 : 1) * (event.shiftKey ? 12 : 1);
  const year = date.getFullYear();
  const month = date.getMonth() + months;
  return new Date(year, month, Math.min(date.getDate(), new Date(year, month + 1, 0).getDate()));
}
