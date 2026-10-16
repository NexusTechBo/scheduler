import { Component, computed, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { formatDayLabel, formatTime } from '../../../core/utils/calendar.util';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-board-card',
  imports: [UiIconComponent],
  templateUrl: './board-card.component.html',
  host: {
    class: 'block rounded-2xl',
    role: 'button',
    tabindex: '0',
    '(keydown.enter)': 'activate($event)',
    '(keydown.space)': 'activate($event)',
  },
})
export class BoardCardComponent {
  appointment = input.required<AppointmentSummaryItem>();
  showDate = input(false);
  draggable = input(true);

  protected activate(event: Event): void {
    event.preventDefault();
    (event.currentTarget as HTMLElement).click();
  }

  protected readonly timeLabel = computed(() => {
    const { startTime, endTime } = this.appointment();
    return `${formatTime(startTime)} – ${formatTime(endTime)}`;
  });
  protected readonly dayLabel = computed(() => formatDayLabel(this.appointment().appointmentDate));
}
