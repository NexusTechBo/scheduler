import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { Component, computed, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatTime, timeSlots } from '../../../core/utils/calendar.util';
import { SelectComponent, SelectOption } from '../select/select.component';
import { UiIconComponent, UiIconName } from '../ui-icon/ui-icon.component';

/**
 * Time dropdown replacing native <input type="time">. Value is a "HH:mm" string (CVA).
 * Reuses app-select's template and listbox/keyboard logic; only the options differ.
 * `min`/`max` are inclusive "HH:mm" bounds; times outside are disabled.
 */
@Component({
  selector: 'app-time-picker',
  imports: [CdkConnectedOverlay, CdkOverlayOrigin, UiIconComponent],
  templateUrl: '../select/select.component.html',
  host: { class: 'block' },
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => TimePickerComponent), multi: true },
  ],
})
export class TimePickerComponent extends SelectComponent<string> {
  /** Minutes between options. */
  step = input(30);
  min = input<string | null>(null);
  max = input<string | null>(null);

  protected override readonly icon: UiIconName = 'clock';

  protected override readonly items = computed<SelectOption<string>[]>(() => {
    const min = this.min();
    const max = this.max();
    const value = this.value();
    let times = timeSlots(this.step());
    // Keep an off-step value (e.g. "08:15" with step 30) visible and selected.
    if (value && !times.includes(value)) times = [...times, value].sort();
    return times.map((time) => ({
      value: time,
      label: formatTime(time),
      disabled: (!!min && time < min) || (!!max && time > max),
    }));
  });
}
