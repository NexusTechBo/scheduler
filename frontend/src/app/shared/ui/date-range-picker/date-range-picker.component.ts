import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { Component, computed, forwardRef, input, output, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  CalendarDay,
  buildMonthMatrix,
  formatLongDate,
  formatMonthLabel,
  formatShortDate,
  parseIso,
  toIso,
} from '../../../core/utils/calendar.util';
import { POPOVER_POSITIONS } from '../overlay-positions';

export interface DateRange {
  from: string;
  to: string;
}

interface DayState {
  isStart: boolean;
  isEnd: boolean;
  inRange: boolean;
  isToday: boolean;
  disabled: boolean;
}

/**
 * Airbnb-style calendar popover, replaces native date inputs.
 * - `range` (default): `[from]`/`[to]` + `(rangeChange)`, for the backend's ?from&to filters.
 * - `single`: one click selects and closes; bind with `formControlName` / `ngModel` (value = ISO date).
 * Rendered in a CDK overlay so it isn't clipped by scrolling dialog bodies.
 */
@Component({
  selector: 'app-date-range-picker',
  imports: [CdkTrapFocus, CdkConnectedOverlay, CdkOverlayOrigin],
  templateUrl: './date-range-picker.component.html',
  host: { class: 'relative' },
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateRangePickerComponent),
      multi: true,
    },
  ],
})
export class DateRangePickerComponent implements ControlValueAccessor {
  mode = input<'range' | 'single'>('range');
  from = input('');
  to = input('');
  /** Inclusive ISO bounds; days outside are disabled. */
  min = input<string | null>(null);
  max = input<string | null>(null);
  /** Field name for the trigger's accessible name, e.g. "Fecha" → "Fecha: 6 oct". */
  ariaLabel = input<string | null>(null);

  rangeChange = output<DateRange>();

  protected readonly weekdayLabels = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
  protected readonly today = toIso(new Date());
  protected readonly formatLongDate = formatLongDate;
  protected readonly positions = POPOVER_POSITIONS;

  protected readonly isOpen = signal(false);
  protected readonly disabled = signal(false);
  /** Single-mode value (CVA). */
  protected readonly value = signal('');
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  private readonly viewDate = signal(new Date());
  protected readonly monthLabel = computed(() => formatMonthLabel(this.viewDate()));
  protected readonly weeks = computed(() =>
    buildMonthMatrix(this.viewDate().getFullYear(), this.viewDate().getMonth()),
  );

  private readonly draftStart = signal<string | null>(null);
  private readonly draftEnd = signal<string | null>(null);
  private readonly hoverIso = signal<string | null>(null);

  protected readonly label = computed(() => {
    if (this.mode() === 'single') {
      return this.value() ? formatShortDate(this.value()) : 'Seleccionar fecha';
    }
    const from = this.from();
    const to = this.to();
    return from === to
      ? formatShortDate(from)
      : `${formatShortDate(from)} – ${formatShortDate(to)}`;
  });

  protected readonly triggerAriaLabel = computed(() => {
    const name = this.ariaLabel();
    return name ? `${name}: ${this.label()}` : null;
  });

  protected readonly todayAllowed = computed(() => !this.isOutOfBounds(this.today));

  writeValue(value: string | null): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  protected toggle(): void {
    this.isOpen() ? this.close() : this.open();
  }

  protected open(): void {
    const single = this.mode() === 'single';
    const start = single ? this.value() || null : this.from();
    this.draftStart.set(start);
    this.draftEnd.set(single ? start : this.to());
    this.viewDate.set(parseIso(start || this.min() || this.today));
    this.isOpen.set(true);
  }

  protected close(): void {
    if (!this.isOpen()) return;
    this.isOpen.set(false);
    this.hoverIso.set(null);
    this.onTouched();
  }

  protected previousMonth(): void {
    this.viewDate.update((date) => new Date(date.getFullYear(), date.getMonth() - 1, 1));
  }

  protected nextMonth(): void {
    this.viewDate.update((date) => new Date(date.getFullYear(), date.getMonth() + 1, 1));
  }

  protected hover(iso: string): void {
    if (this.mode() === 'range') this.hoverIso.set(iso);
  }

  protected clearHover(): void {
    this.hoverIso.set(null);
  }

  protected pick(iso: string): void {
    if (this.isOutOfBounds(iso)) return;
    if (this.mode() === 'single') {
      this.value.set(iso);
      this.onChange(iso);
      this.close();
      return;
    }

    const start = this.draftStart();
    if (!start || this.draftEnd()) {
      this.draftStart.set(iso);
      this.draftEnd.set(null);
      return;
    }

    this.commit(iso < start ? { from: iso, to: start } : { from: start, to: iso });
  }

  protected presetToday(): void {
    if (this.mode() === 'single') {
      this.pick(this.today);
      return;
    }
    this.commit({ from: this.today, to: this.today });
  }

  protected presetNext7Days(): void {
    const start = new Date();
    const end = new Date();
    end.setDate(end.getDate() + 6);
    this.commit({ from: toIso(start), to: toIso(end) });
  }

  protected presetThisMonth(): void {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.commit({ from: toIso(start), to: toIso(end) });
  }

  private commit(range: DateRange): void {
    this.draftStart.set(range.from);
    this.draftEnd.set(range.to);
    this.rangeChange.emit(range);
    this.close();
  }

  private isOutOfBounds(iso: string): boolean {
    const min = this.min();
    const max = this.max();
    return (!!min && iso < min) || (!!max && iso > max);
  }

  protected dayState(day: CalendarDay): DayState {
    const start = this.draftStart();
    const end = this.draftEnd() ?? this.hoverIso();
    const lo = start && end ? (start < end ? start : end) : null;
    const hi = start && end ? (start < end ? end : start) : null;

    return {
      isStart: day.iso === start,
      isEnd: day.iso === this.draftEnd(),
      inRange: !!lo && !!hi && day.iso > lo && day.iso < hi,
      isToday: day.iso === this.today,
      disabled: this.isOutOfBounds(day.iso),
    };
  }
}
