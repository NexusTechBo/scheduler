import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { TimeRangePipe } from '../../pipes/time-range.pipe';
import { KeyboardActivateDirective } from '../../ui/keyboard-activate/keyboard-activate.directive';
import { UiIconComponent } from '../../ui/ui-icon/ui-icon.component';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';

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
