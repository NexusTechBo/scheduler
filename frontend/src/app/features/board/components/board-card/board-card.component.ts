import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../../core/models/api.models';
import { formatDayLabel } from '../../../../core/utils/calendar.util';
import { TimeRangePipe } from '../../../../shared/pipes/time-range.pipe';
import { KeyboardActivateDirective } from '../../../../shared/ui/keyboard-activate.directive';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-board-card',
  imports: [UiIconComponent, TimeRangePipe],
  templateUrl: './board-card.component.html',
  host: { class: 'block rounded-2xl' },
  hostDirectives: [KeyboardActivateDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardCardComponent {
  appointment = input.required<AppointmentSummaryItem>();
  showDate = input(false);
  draggable = input(true);

  protected readonly dayLabel = computed(() => formatDayLabel(this.appointment().appointmentDate));
}
