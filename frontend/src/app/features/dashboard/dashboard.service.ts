import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { forkJoin, map } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { resourceError } from '../../core/http/api-error';
import { AppointmentSummaryItem, Role } from '../../core/models/api.models';
import {
  addDays,
  formatShortDate,
  formatWeekdayShort,
  startOfWeek,
  toIso,
  toLocalDateTime,
} from '../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../shared/appointments/appointment-actions.service';
import { UiIconName } from '../../shared/ui/ui-icon/ui-icon.component';

interface Kpi {
  label: string;
  value: number | null;
  icon: UiIconName;
  tone: string;
}

export interface WeekDayStats {
  iso: string;
  weekday: string;
  day: number;
  isToday: boolean;
  confirmed: number;
  pending: number;
  cancelled: number;
  total: number;
}

const PENDING_LIMIT = 6;

@Injectable()
export class DashboardService {
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly role = inject(AuthService).role();
  private readonly today = toIso(new Date());
  private readonly weekStart = startOfWeek(new Date());

  private readonly weekResource = rxResource({
    loader: () =>
      this.appointmentsApi.board({
        from: toIso(this.weekStart),
        to: toIso(addDays(this.weekStart, 6)),
      }),
  });

  private readonly staffResource = rxResource({
    request: () => (this.role === 'ADMINISTRATOR' ? true : undefined),
    loader: () =>
      forkJoin({
        doctors: this.countActive('DOCTOR'),
        assistants: this.countActive('ASSISTANT'),
      }),
  });

  readonly isWeekLoading = this.weekResource.isLoading;
  readonly weekErrorMessage = resourceError(this.weekResource, 'No se pudo cargar la agenda');

  readonly todayAppointments = computed<AppointmentSummaryItem[]>(() => {
    const board = this.weekResource.hasValue() ? this.weekResource.value() : undefined;
    if (!board) return [];
    return [...board.PENDING, ...board.CONFIRMED]
      .filter((appointment) => appointment.appointmentDate === this.today)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  /** Upcoming appointments of the week still waiting for confirmation, soonest first. */
  private readonly upcomingPending = computed<AppointmentSummaryItem[]>(() => {
    const board = this.weekResource.hasValue() ? this.weekResource.value() : undefined;
    if (!board) return [];
    return board.PENDING.filter((appointment) => appointment.appointmentDate >= this.today).sort(
      (a, b) =>
        a.appointmentDate.localeCompare(b.appointmentDate) || a.startTime.localeCompare(b.startTime),
    );
  });
  readonly pendingCount = computed(() => this.upcomingPending().length);
  readonly pendingToConfirm = computed(() =>
    this.upcomingPending()
      .slice(0, PENDING_LIMIT)
      .map((appointment) => ({
        appointment,
        dayLabel:
          appointment.appointmentDate === this.today
            ? 'Hoy'
            : formatShortDate(appointment.appointmentDate),
      })),
  );

  readonly weekDays = computed<WeekDayStats[]>(() => {
    const board = this.weekResource.hasValue() ? this.weekResource.value() : undefined;
    return Array.from({ length: 7 }, (_, i) => {
      const date = addDays(this.weekStart, i);
      const iso = toIso(date);
      const count = (items: AppointmentSummaryItem[] | undefined) =>
        items?.filter((appointment) => appointment.appointmentDate === iso).length ?? 0;
      const confirmed = count(board?.CONFIRMED);
      const pending = count(board?.PENDING);
      const cancelled = count(board?.CANCELLED);
      return {
        iso,
        weekday: formatWeekdayShort(date),
        day: date.getDate(),
        isToday: iso === this.today,
        confirmed,
        pending,
        cancelled,
        total: confirmed + pending + cancelled,
      };
    });
  });
  readonly weekTotal = computed(() => this.weekDays().reduce((sum, day) => sum + day.total, 0));

  readonly kpis = computed<Kpi[]>(() => {
    const board = this.weekResource.hasValue() ? this.weekResource.value() : undefined;
    const staff = this.staffResource.hasValue() ? this.staffResource.value() : null;
    const kpis: Kpi[] = [
      {
        label: 'Citas hoy',
        value: board ? this.todayAppointments().length : null,
        icon: 'calendar',
        tone: 'bg-primary/10 text-primary',
      },
      {
        label: 'Pendientes (semana)',
        value: board?.PENDING.length ?? null,
        icon: 'clock',
        tone: 'bg-tertiary/10 text-tertiary',
      },
      {
        label: 'Confirmadas (semana)',
        value: board?.CONFIRMED.length ?? null,
        icon: 'check',
        tone: 'bg-secondary/10 text-secondary',
      },
      {
        label: 'Canceladas (semana)',
        value: board?.CANCELLED.length ?? null,
        icon: 'block',
        tone: 'bg-gray-900/[0.06] text-gray-500',
      },
    ];
    return kpis;
  });

  reload(): void {
    this.weekResource.reload();
  }

  open(appointment: AppointmentSummaryItem): void {
    this.actions.openDetail(appointment.id).subscribe(() => this.reload());
  }

  confirm(appointment: AppointmentSummaryItem): void {
    this.actions
      .confirm(appointment.id, appointment.clientName, startOf(appointment))
      .subscribe(() => this.reload());
  }

  cancel(appointment: AppointmentSummaryItem): void {
    this.actions
      .cancel(appointment.id, appointment.clientName, startOf(appointment))
      .subscribe(() => this.reload());
  }

  private countActive(role: Role) {
    return this.staffApi
      .list({ role, isActive: true, size: 1 })
      .pipe(map((page) => page.page.totalElements));
  }
}

function startOf(appointment: AppointmentSummaryItem): string {
  return toLocalDateTime(appointment.appointmentDate, appointment.startTime);
}
