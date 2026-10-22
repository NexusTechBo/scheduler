import {
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatTime, timeSlots } from '../../../../core/utils/calendar.util';
import { SelectComponent, SelectOption } from '../select/select.component';

const STEP_MINUTES = 30;

@Component({
  selector: 'app-time-picker',
  imports: [FormsModule, SelectComponent],
  templateUrl: './time-picker.component.html',
  host: { class: 'block', '[attr.id]': 'null', '(focusout)': 'onTouched()' },
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => TimePickerComponent), multi: true },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimePickerComponent implements ControlValueAccessor {
  readonly min = input<string | null>(null);
  readonly id = input<string | null>(null);

  protected readonly value = signal<string | null>(null);
  protected readonly disabled = signal(false);

  protected readonly options = computed<SelectOption<string>[]>(() => {
    const min = this.min();
    const value = this.value();
    let times = timeSlots(STEP_MINUTES);
    if (value && !times.includes(value)) times = [...times, value].sort();
    return times.map((time) => ({
      value: time,
      label: time,
      disabled: !!min && time < min,
    }));
  });

  private onChange: (value: string) => void = () => {};
  protected onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    this.value.set(value ? formatTime(value) : null);
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

  protected change(value: string): void {
    this.value.set(value);
    this.onChange(value);
  }
}
