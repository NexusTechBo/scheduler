import { Component, computed, input } from '@angular/core';
import { AppointmentStatus } from '../../../core/models/api.models';
import { STATUS_LABEL, STATUS_STYLE } from '../../../core/utils/labels.util';

@Component({
  selector: 'app-status-badge',
  template: `<span class="chip" [class]="style().badge">
    <span class="h-1.5 w-1.5 rounded-full" [class]="style().dot"></span>
    {{ label() }}
  </span>`
})
export class StatusBadgeComponent {
  status = input.required<AppointmentStatus>();

  protected readonly style = computed(() => STATUS_STYLE[this.status()]);
  protected readonly label = computed(() => STATUS_LABEL[this.status()]);
}
