import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-calendar-nav',
  imports: [UiIconComponent],
  templateUrl: './calendar-nav.component.html',
  host: { class: 'flex items-center gap-2.5' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarNavComponent {
  previous = output<void>();
  next = output<void>();
  today = output<void>();
}
