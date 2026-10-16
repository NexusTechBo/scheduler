import { Component, computed, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { formatTime } from '../../../core/utils/calendar.util';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-appointment-list-item',
  imports: [StatusBadgeComponent, UiIconComponent],
  templateUrl: './appointment-list-item.component.html',
  host: {
    class: 'block rounded-2xl',
    role: 'button',
    tabindex: '0',
    '(keydown.enter)': 'activate($event)',
    '(keydown.space)': 'activate($event)',
  },
})
export class AppointmentListItemComponent {
  appointment = input.required<AppointmentSummaryItem>();
  doctorAsTitle = input(false);

  protected activate(event: Event): void {
    event.preventDefault();
    (event.currentTarget as HTMLElement).click();
  }

  protected readonly timeLabel = computed(() => {
    const { startTime, endTime } = this.appointment();
    return `${formatTime(startTime)} – ${formatTime(endTime)}`;
  });
}
