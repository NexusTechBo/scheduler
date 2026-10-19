import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { Component, computed, forwardRef, input } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatTime, timeSlots } from '../../../core/utils/calendar.util';
import { SelectComponent, SelectOption } from '../select/select.component';
import { UiIconComponent, UiIconName } from '../ui-icon/ui-icon.component';

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
  step = input(30);
  min = input<string | null>(null);
  max = input<string | null>(null);

  protected override readonly icon: UiIconName = 'clock';

  protected override readonly items = computed<SelectOption<string>[]>(() => {
    const min = this.min();
    const max = this.max();
    const value = this.value();
    let times = timeSlots(this.step());
    if (value && !times.includes(value)) times = [...times, value].sort();
    return times.map((time) => ({
      value: time,
      label: formatTime(time),
      disabled: (!!min && time < min) || (!!max && time > max),
    }));
  });
}
