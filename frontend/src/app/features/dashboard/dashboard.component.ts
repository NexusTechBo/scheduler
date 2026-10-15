import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin, map, of } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { AppointmentSummaryItem, ROLE_ID } from '../../core/models/api.models';
import { ROLES } from '../../core/navigation/navigation';
import { addDays, startOfWeek, toIso } from '../../core/utils/calendar.util';
import { AppointmentListItemComponent } from '../../shared/ui/appointment-list-item/appointment-list-item.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent, UiIconName } from '../../shared/ui/ui-icon/ui-icon.component';
import { AppointmentActionsService } from '../appointments/appointment-actions.service';

interface Kpi {
  label: string;
  value: number;
  icon: UiIconName;
  tone: string;
}

interface QuickLink {
  label: string;
  description: string;
  route: string;
  icon: UiIconName;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    PageHeaderComponent,
    AppointmentListItemComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  templateUrl: './dashboard.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class DashboardComponent {
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly auth = inject(AuthService);
  private readonly actions = inject(AppointmentActionsService);

  protected readonly isAdmin = this.auth.hasRole('ADMINISTRATOR');
  private readonly today = toIso(new Date());
  private readonly weekStart = startOfWeek(new Date());

  protected readonly weekResource = rxResource({
    loader: () =>
      this.appointmentsApi.board({
        from: toIso(this.weekStart),
        to: toIso(addDays(this.weekStart, 6)),
      }),
  });

  /** Administrators also get a staff overview (GET /personal is ADMINISTRATOR only). */
  private readonly staffResource = rxResource({
    loader: () =>
      this.isAdmin
        ? forkJoin({
            doctors: this.staffApi
              .list({ roleId: ROLE_ID.DOCTOR, isActive: true, size: 1 })
              .pipe(map((page) => page.page.totalElements)),
            assistants: this.staffApi
              .list({ roleId: ROLE_ID.ASSISTANT, isActive: true, size: 1 })
              .pipe(map((page) => page.page.totalElements)),
          })
        : of(null),
  });

  protected readonly todayAppointments = computed<AppointmentSummaryItem[]>(() => {
    const board = this.weekResource.value();
    if (!board) return [];
    return [...board.PENDING, ...board.CONFIRMED]
      .filter((appointment) => appointment.appointmentDate === this.today)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  });

  protected readonly kpis = computed<Kpi[]>(() => {
    const board = this.weekResource.value();
    const staff = this.staffResource.value();
    const kpis: Kpi[] = [
      {
        label: 'Citas hoy',
        value: this.todayAppointments().length,
        icon: 'calendar',
        tone: 'bg-primary/10 text-primary',
      },
      {
        label: 'Pendientes (semana)',
        value: board?.PENDING.length ?? 0,
        icon: 'clock',
        tone: 'bg-tertiary/10 text-tertiary',
      },
      {
        label: 'Confirmadas (semana)',
        value: board?.CONFIRMED.length ?? 0,
        icon: 'check',
        tone: 'bg-secondary/10 text-secondary',
      },
      {
        label: 'Canceladas (semana)',
        value: board?.CANCELLED.length ?? 0,
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

  protected readonly quickLinks: QuickLink[] = [
    ...(this.auth.hasRole(...ROLES.clinical)
      ? [
          {
            label: 'Agendar cita',
            description: 'Reserva un horario para un paciente',
            route: '/appointments/new',
            icon: 'plus' as UiIconName,
          },
        ]
      : []),
    {
      label: 'Tablero',
      description: 'Confirma o cancela citas pendientes',
      route: '/board',
      icon: 'board',
    },
    ...(this.auth.hasRole(...ROLES.clinical)
      ? [
          {
            label: 'Pacientes',
            description: 'Registra y asigna pacientes',
            route: '/patients',
            icon: 'patients' as UiIconName,
          },
        ]
      : []),
    ...(this.auth.hasRole('DOCTOR')
      ? [
          {
            label: 'Mi disponibilidad',
            description: 'Horario semanal y bloqueos',
            route: '/availability',
            icon: 'clock' as UiIconName,
          },
        ]
      : []),
    ...(this.isAdmin
      ? [
          {
            label: 'Personal',
            description: 'Doctores y asistentes de la clínica',
            route: '/staff',
            icon: 'staff' as UiIconName,
          },
          {
            label: 'Especialidades',
            description: 'Catálogo de especialidades',
            route: '/specialties',
            icon: 'tag' as UiIconName,
          },
        ]
      : []),
  ];

  protected open(appointment: AppointmentSummaryItem): void {
    this.actions
      .openDetail(appointment.id)
      .subscribe((changed) => changed && this.weekResource.reload());
  }
}
