import { CdkTrapFocus } from '@angular/cdk/a11y';
import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatShortDate, parseIso, toIso } from '../../../../core/utils/calendar.util';
import { POPOVER_POSITIONS } from '../../overlay-positions';
import { UiIconComponent } from '../../ui-icon/ui-icon.component';
import { MonthGridComponent } from '../month-grid/month-grid.component';

@Component({
  selector: 'app-date-picker',
  imports: [
    CdkTrapFocus,
    CdkConnectedOverlay,
    CdkOverlayOrigin,
    MonthGridComponent,
    UiIconComponent,
  ],
  templateUrl: './date-picker.component.html',
  host: { class: 'relative', '[attr.id]': 'null' },
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DatePickerComponent), multi: true },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DatePickerComponent implements ControlValueAccessor {
  readonly min = input<string | null>(null);
  readonly ariaLabel = input<string | null>(null);
  readonly id = input<string | null>(null);

  protected readonly today = toIso(new Date());
  protected readonly positions = POPOVER_POSITIONS;
  protected readonly isOpen = signal(false);
  protected readonly disabled = signal(false);
  protected readonly value = signal<string | null>(null);
  protected readonly viewDate = signal(new Date());
  protected readonly label = computed(() => {
    const value = this.value();
    return value ? formatShortDate(value) : 'Seleccionar fecha';
  });

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    this.value.set(value || null);
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

  protected open(): void {
    this.viewDate.set(parseIso(this.value() ?? this.min() ?? this.today));
    this.isOpen.set(true);
  }

  protected close(): void {
    if (!this.isOpen()) return;
    this.isOpen.set(false);
    this.onTouched();
  }

  protected pick(iso: string): void {
    if (this.outOfBounds(iso)) return;
    this.value.set(iso);
    this.onChange(iso);
    this.close();
  }

  protected outOfBounds(iso: string): boolean {
    const min = this.min();
    return !!min && iso < min;
  }
}
