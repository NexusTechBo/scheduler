import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { forkJoin, map } from 'rxjs';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { resourceError } from '../../../core/http/api-error';
import { AppointmentSummaryItem, Role } from '../../../core/models/api.models';
import { HOME_URL, navItemsFor } from '../../../core/navigation/navigation';
import { addDays, startOfWeek, toIso } from '../../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../../shared/appointments/services/appointment-actions.service';
import { UiIconName } from '../../../shared/ui/ui-icon/ui-icon.component';

interface Kpi {
  label: string;
  value: number | null;
  icon: UiIconName;
  tone: string;
}

@Injectable()
export class DashboardService {
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly role = inject(AuthService).role();
  private readonly today = toIso(new Date());
  private readonly weekStart = startOfWeek(new Date());

  readonly quickLinks = navItemsFor(this.role).filter((item) => item.route !== HOME_URL);

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
    if (staff) {
      kpis.push(
        {
          label: 'Doctores activos',
          value: staff.doctors,
          icon: 'stethoscope',
          tone: 'bg-primary/10 text-primary',
        },
        {
          label: 'Asistentes activos',
          value: staff.assistants,
          icon: 'staff',
          tone: 'bg-secondary/10 text-secondary',
        },
      );
    }
    return kpis;
  });

  reload(): void {
    this.weekResource.reload();
  }

  open(appointment: AppointmentSummaryItem): void {
    this.actions.openDetail(appointment.id).subscribe(() => this.reload());
  }

  private countActive(role: Role) {
    return this.staffApi
      .list({ role, isActive: true, size: 1 })
      .pipe(map((page) => page.page.totalElements));
  }
}
