import { Component, computed, input } from '@angular/core';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import { formatTime } from '../../../core/utils/calendar.util';
import { StatusBadgeComponent } from '../status-badge/status-badge.component';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-appointment-list-item',
  imports: [StatusBadgeComponent, UiIconComponent],
  templateUrl: './appointment-list-item.component.html',
  // Consumers bind (click) on the host; Enter/Space re-dispatch it so keyboard users get the same action.
  // Focus outline comes from the global :focus-visible rule in styles.css.
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
  /** Patients already know who they are: show the doctor as the title instead. */
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
