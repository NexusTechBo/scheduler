import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { ChangeDetectionStrategy, Component, computed, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { addDays, formatShortDate, parseIso, toIso } from '../../../../core/utils/calendar.util';
import { POPOVER_POSITIONS } from '../../../utils/overlay-positions.util';
import { UiIconComponent } from '../../ui-icon/ui-icon.component';
import { MonthGridComponent } from '../month-grid/month-grid.component';

export interface DateRange {
  from: string;
  to: string;
}

@Component({
  selector: 'app-date-range-picker',
  imports: [
    CdkTrapFocus,
    CdkConnectedOverlay,
    CdkOverlayOrigin,
    MonthGridComponent,
    UiIconComponent,
  ],
  templateUrl: './date-range-picker.component.html',
  host: { class: 'relative' },
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateRangePickerComponent),
      multi: true,
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateRangePickerComponent implements ControlValueAccessor {
  protected readonly positions = POPOVER_POSITIONS;
  protected readonly presets: { label: string; range: () => DateRange }[] = [
    { label: 'Hoy', range: () => ({ from: toIso(new Date()), to: toIso(new Date()) }) },
    {
      label: 'Próximos 7 días',
      range: () => ({ from: toIso(new Date()), to: toIso(addDays(new Date(), 6)) }),
    },
    {
      label: 'Este mes',
      range: () => {
        const now = new Date();
        return {
          from: toIso(new Date(now.getFullYear(), now.getMonth(), 1)),
          to: toIso(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
        };
      },
    },
  ];

  protected readonly isOpen = signal(false);
  protected readonly disabled = signal(false);
  protected readonly value = signal<DateRange | null>(null);
  protected readonly viewDate = signal(new Date());
  protected readonly draftStart = signal<string | null>(null);
  protected readonly draftEnd = signal<string | null>(null);
  protected readonly hovered = signal<string | null>(null);

  protected readonly label = computed(() => {
    const range = this.value();
    if (!range) return 'Seleccionar fechas';
    return range.from === range.to
      ? formatShortDate(range.from)
      : `${formatShortDate(range.from)} – ${formatShortDate(range.to)}`;
  });

  private onChange: (value: DateRange) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: DateRange | null): void {
    this.value.set(value);
  }

  registerOnChange(fn: (value: DateRange) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  protected open(): void {
    const range = this.value();
    this.draftStart.set(range?.from ?? null);
    this.draftEnd.set(range?.to ?? null);
    this.viewDate.set(parseIso(range?.from ?? toIso(new Date())));
    this.isOpen.set(true);
  }

  protected close(): void {
    if (!this.isOpen()) return;
    this.isOpen.set(false);
    this.hovered.set(null);
    this.onTouched();
  }

  protected pick(iso: string): void {
    const start = this.draftStart();
    if (!start || this.draftEnd()) {
      this.draftStart.set(iso);
      this.draftEnd.set(null);
      return;
    }
    this.commit(iso < start ? { from: iso, to: start } : { from: start, to: iso });
  }

  protected commit(range: DateRange): void {
    this.value.set(range);
    this.onChange(range);
    this.close();
  }
}
