import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AppointmentStatus } from '../../../core/models/api.models';
import { STATUS_LABEL, STATUS_STYLE } from '../../../core/utils/labels.util';

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  status = input.required<AppointmentStatus>();

  protected readonly style = computed(() => STATUS_STYLE[this.status()]);
  protected readonly label = computed(() => STATUS_LABEL[this.status()]);
}
