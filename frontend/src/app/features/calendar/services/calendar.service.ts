import { Injectable, computed, inject } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { AuthService } from '../../../core/auth/auth.service';
import { canBookAppointments } from '../../../core/auth/permissions';
import { AppointmentSummaryItem } from '../../../core/models/api.models';
import {
  buildMonthMatrix,
  calendarKeyToIso,
  formatDayLabel,
  formatMonthLabel,
  isIsoDate,
  parseIso,
  toIso,
} from '../../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../../shared/appointments/services/appointment-actions.service';

function firstOfMonth(iso: string): Date {
  const date = parseIso(iso);
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

@Injectable()
export class CalendarService {
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly canBook = canBookAppointments(inject(AuthService).role());
  private readonly today = toIso(new Date());

  private readonly dateParam = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('date'))),
  );
  readonly selectedDate = computed(() => {
    const requested = this.dateParam();
    return isIsoDate(requested) ? requested : this.today;
  });
  private readonly viewDate = computed(() => firstOfMonth(this.selectedDate()));

  readonly monthLabel = computed(() => formatMonthLabel(this.viewDate()));
  readonly weeks = computed(() =>
    buildMonthMatrix(this.viewDate().getFullYear(), this.viewDate().getMonth()),
  );

  private readonly resource = rxResource({
    request: () => ({ month: this.viewDate().getMonth() + 1, year: this.viewDate().getFullYear() }),
    loader: ({ request }) => this.api.calendar(request),
  });
  readonly isLoading = this.resource.isLoading;
  readonly hasError = computed(() => !!this.resource.error());

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

  readonly selectedDayLabel = computed(() => formatDayLabel(this.selectedDate()));
  readonly selectedDayAppointments = computed(() => this.appointmentsFor(this.selectedDate()));
  readonly canCreateOnSelected = computed(() => this.canBook && this.selectedDate() >= this.today);

  appointmentsFor(iso: string): AppointmentSummaryItem[] | null {
    if (!this.resource.hasValue()) return null;
    return this.byDay().get(iso) ?? [];
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
