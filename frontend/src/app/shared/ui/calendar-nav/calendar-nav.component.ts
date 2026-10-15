import { Component, output } from '@angular/core';

@Component({
  selector: 'app-calendar-nav',
  imports: [],
  templateUrl: './calendar-nav.component.html',
  host: { class: 'flex items-center gap-2.5' }
})
export class CalendarNavComponent {
  previous = output<void>();
  next = output<void>();
  today = output<void>();
}
