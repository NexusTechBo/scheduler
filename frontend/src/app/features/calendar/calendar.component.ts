import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DAYS_OF_WEEK } from '../../core/utils/labels.util';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { CalendarService } from './services/calendar.service';
import { CalendarDayCellComponent } from './components/calendar-day-cell/calendar-day-cell.component';
import { CalendarNavComponent } from './components/calendar-nav/calendar-nav.component';
import { DayAgendaPanelComponent } from './components/day-agenda-panel/day-agenda-panel.component';

@Component({
  selector: 'app-calendar',
  imports: [
    LoadErrorComponent,
    CalendarDayCellComponent,
    PageHeaderComponent,
    CalendarNavComponent,
    DayAgendaPanelComponent,
  ],
  providers: [CalendarService],
  templateUrl: './calendar.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarComponent {
  protected readonly calendar = inject(CalendarService);
  protected readonly weekdays = DAYS_OF_WEEK;
}
