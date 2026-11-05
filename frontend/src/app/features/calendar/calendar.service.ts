import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { AuthService } from '../../core/auth/auth.service';
import { resourceError } from '../../core/http/api-error';
import { AppointmentSummaryItem } from '../../core/models/api.models';
import {
  buildMonthMatrix,
  calendarKeyToIso,
  formatDayLabel,
  formatMonthLabel,
  isIsoDate,
  parseIso,
  toIso,
} from '../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../shared/services/appointment-actions.service';
import { CalendarCell, CalendarScope } from './calendar.model';

@Injectable()
export class CalendarService {
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly today = toIso(new Date());
  private readonly _scope = signal<CalendarScope>('all');

  readonly canFilterMine = computed(
    () => this.auth.hasRole('ADMINISTRATOR') && this.auth.hasRole('DOCTOR'),
  );
  readonly scope = computed<CalendarScope>(() => (this.canFilterMine() ? this._scope() : 'all'));

  private readonly dateParam = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('date'))),
  );
  readonly selectedDate = computed(() => {
    const requested = this.dateParam();
    return isIsoDate(requested) ? requested : this.today;
  });
  private readonly monthKey = computed(() => this.selectedDate().slice(0, 7));
  private readonly viewDate = computed(() => parseIso(`${this.monthKey()}-01`));

  readonly monthLabel = computed(() => formatMonthLabel(this.viewDate()));
  private readonly weeks = computed(() =>
    buildMonthMatrix(this.viewDate().getFullYear(), this.viewDate().getMonth()),
  );

  private readonly resource = rxResource({
    params: () => {
      const [year, month] = this.monthKey().split('-').map(Number);
      const doctorId = this.scope() === 'mine' ? (this.auth.userId() ?? undefined) : undefined;
      return { month, year, doctorId };
    },
    stream: ({ params }) => this.api.calendar(params),
  });
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, 'No se pudo cargar el calendario');

  private readonly byDay = computed(() => {
    const result = new Map<string, AppointmentSummaryItem[]>();
    const calendar = this.resource.hasValue() ? this.resource.value() : {};
    for (const [key, items] of Object.entries(calendar ?? {})) {
      result.set(
        calendarKeyToIso(key),
        [...items].sort((a, b) => a.startTime.localeCompare(b.startTime)),
      );
    }
    return result;
  });

  readonly cells = computed<CalendarCell[][]>(() => {
    const loaded = this.resource.hasValue();
    const byDay = this.byDay();
    return this.weeks().map((week) =>
      week.map((day) => ({ day, appointments: loaded ? (byDay.get(day.iso) ?? []) : null })),
    );
  });

  readonly selectedDayLabel = computed(() => formatDayLabel(this.selectedDate()));
  readonly selectedDayAppointments = computed(() =>
    this.resource.hasValue() ? (this.byDay().get(this.selectedDate()) ?? []) : null,
  );
  readonly canCreateOnSelected = computed(() => this.selectedDate() >= this.today);

  reload(): void {
    this.resource.reload();
  }

  setScope(scope: CalendarScope): void {
    this._scope.set(scope);
  }

  select(iso: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { date: iso },
      replaceUrl: true,
    });
  }

  shiftMonth(delta: number): void {
    const view = this.viewDate();
    const first = toIso(new Date(view.getFullYear(), view.getMonth() + delta, 1));
    this.select(first.slice(0, 7) === this.today.slice(0, 7) ? this.today : first);
  }

  goToday(): void {
    this.select(this.today);
  }

  openNewAppointment(): void {
    this.router
      .navigate(['/appointments/new'], { queryParams: { date: this.selectedDate() } })
      .finally();
  }

  open(appointment: AppointmentSummaryItem): void {
    this.actions.openDetail(appointment.id).subscribe(() => this.resource.reload());
  }
}
