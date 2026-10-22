import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { resourceError } from '../../../core/http/api-error';
import {
  AppointmentFilters,
  AppointmentResponse,
  AppointmentStatus,
} from '../../../core/models/api.models';
import { datePart, formatDayLabel, formatTimeRange } from '../../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../../shared/appointments/services/appointment-actions.service';

const PAGE_SIZE = 12;

export type AppointmentStatusFilter = AppointmentStatus | 'ALL';

export interface AppointmentRow extends AppointmentResponse {
  dayLabel: string;
  timeLabel: string;
}

@Injectable()
export class AppointmentsPageService {
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);

  private readonly filters = signal<AppointmentFilters>({
    page: 0,
    size: PAGE_SIZE,
    sort: 'startTime,desc',
  });

  private readonly resource = rxResource({
    request: () => this.filters(),
    loader: ({ request }) => this.api.list(request),
  });

  readonly appointments = computed<AppointmentRow[]>(() =>
    (this.resource.value()?.content ?? []).map((appointment) => ({
      ...appointment,
      dayLabel: formatDayLabel(datePart(appointment.startTime)),
      timeLabel: formatTimeRange(appointment.startTime, appointment.endTime),
    })),
  );
  readonly pagination = computed(() => this.resource.value()?.page ?? null);
  readonly status = computed<AppointmentStatusFilter>(() => this.filters().status ?? 'ALL');
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, 'No se pudieron cargar las citas');

  reload(): void {
    this.resource.reload();
  }

  setStatus(status: AppointmentStatusFilter): void {
    this.filters.update((current) => ({
      ...current,
      page: 0,
      status: status === 'ALL' ? undefined : status,
    }));
  }

  setPage(page: number): void {
    this.filters.update((current) => ({ ...current, page }));
  }

  open(row: AppointmentRow): void {
    this.actions.openDetail(row.id).subscribe(() => this.reload());
  }

  confirm(row: AppointmentRow): Observable<unknown> {
    return this.actions
      .confirm(row.id, row.patientName, row.startTime)
      .pipe(tap(() => this.reload()));
  }

  cancel(row: AppointmentRow): Observable<unknown> {
    return this.actions
      .cancel(row.id, row.patientName, row.startTime)
      .pipe(tap(() => this.reload()));
  }
}
