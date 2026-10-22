import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { TimeRangePipe } from '../../pipes/time-range.pipe';
import { KeyboardActivateDirective } from '../keyboard-activate.directive';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-appointment-list-item',
  imports: [StatusBadgeComponent, UiIconComponent, TimeRangePipe],
  templateUrl: './appointment-list-item.component.html',
  host: { class: 'block rounded-2xl' },
  hostDirectives: [KeyboardActivateDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentListItemComponent {
  appointment = input.required<AppointmentSummaryItem>();
}
