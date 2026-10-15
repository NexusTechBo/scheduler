import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentSummaryItem } from '../../core/models/api.models';
import { ROLES } from '../../core/navigation/navigation';
import {
  buildMonthMatrix,
  calendarKeyToIso,
  formatDayLabel,
  formatMonthLabel,
  toIso,
} from '../../core/utils/calendar.util';
import { CalendarDayCellComponent } from '../../shared/ui/calendar-day-cell/calendar-day-cell.component';
import { CalendarNavComponent } from '../../shared/ui/calendar-nav/calendar-nav.component';
import { DayAgendaPanelComponent } from '../../shared/ui/day-agenda-panel/day-agenda-panel.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { AppointmentActionsService } from '../appointments/appointment-actions.service';

@Component({
  selector: 'app-calendar',
  imports: [
    CalendarDayCellComponent,
    PageHeaderComponent,
    CalendarNavComponent,
    DayAgendaPanelComponent,
  ],
  templateUrl: './calendar.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class CalendarComponent {
  private readonly api = inject(AppointmentsApi);
  private readonly auth = inject(AuthService);
  private readonly actions = inject(AppointmentActionsService);
  private readonly router = inject(Router);

  protected readonly weekdayLabels = ['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM'];
  protected readonly isPatient = this.auth.hasRole('PATIENT');
  private readonly canBook = this.auth.hasRole(...ROLES.booking);
  private readonly today = toIso(new Date());

  private readonly viewDate = signal(new Date());
  protected readonly selectedDate = signal(this.today);

  protected readonly monthLabel = computed(() => formatMonthLabel(this.viewDate()));
  protected readonly weeks = computed(() =>
    buildMonthMatrix(this.viewDate().getFullYear(), this.viewDate().getMonth()),
  );

  /** The backend returns one month; days of the neighbour months shown in the grid simply have no data. */
  protected readonly resource = rxResource({
    request: () => ({ month: this.viewDate().getMonth() + 1, year: this.viewDate().getFullYear() }),
    loader: ({ request }) => this.api.calendar(request),
  });

  private readonly byDay = computed(() => {
    const result = new Map<string, AppointmentSummaryItem[]>();
    for (const [key, items] of Object.entries(this.resource.value() ?? {})) {
      result.set(
        calendarKeyToIso(key),
        [...items].sort((a, b) => a.startTime.localeCompare(b.startTime)),
      );
    }
    return result;
  });

  protected readonly selectedDayLabel = computed(() => formatDayLabel(this.selectedDate()));
  protected readonly selectedDayAppointments = computed(
    () => this.byDay().get(this.selectedDate()) ?? [],
  );
  protected readonly canCreateOnSelected = computed(
    () => this.canBook && this.selectedDate() >= this.today,
  );

  protected appointmentsFor(iso: string): AppointmentSummaryItem[] {
    return this.byDay().get(iso) ?? [];
  }

  protected previousMonth(): void {
    this.viewDate.update((date) => new Date(date.getFullYear(), date.getMonth() - 1, 1));
  }

  protected nextMonth(): void {
    this.viewDate.update((date) => new Date(date.getFullYear(), date.getMonth() + 1, 1));
  }

  protected goToday(): void {
    this.viewDate.set(new Date());
    this.selectedDate.set(this.today);
  }

  protected selectDay(iso: string): void {
    this.selectedDate.set(iso);
    const [year, month] = iso.split('-').map(Number);
    const view = this.viewDate();
    if (view.getFullYear() !== year || view.getMonth() !== month - 1)
      this.viewDate.set(new Date(year, month - 1, 1));
  }

  protected openNewAppointment(): void {
    this.router.navigate(['/appointments/new'], { queryParams: { date: this.selectedDate() } });
  }

  protected open(appointment: AppointmentSummaryItem): void {
    this.actions
      .openDetail(appointment.id)
      .subscribe((changed) => changed && this.resource.reload());
  }
}
