import { computed, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { AppointmentsApi } from '../../../core/api/appointments.api';
import { apiErrorMessage } from '../../../core/http/api-error';
import {
  AppointmentFilters,
  AppointmentResponse,
  AppointmentStatus,
} from '../../../core/models/api.models';
import { datePart, formatDayLabel, formatTime } from '../../../core/utils/calendar.util';

export const APPOINTMENTS_PAGE_SIZE = 12;

export type AppointmentStatusFilter = AppointmentStatus | 'ALL';

export interface AppointmentRow extends AppointmentResponse {
  dayLabel: string;
  timeLabel: string;
}

export const getDefaultAppointmentFilters = (): AppointmentFilters => ({
  page: 0,
  size: APPOINTMENTS_PAGE_SIZE,
  sort: 'startTime,desc',
});

/** Server state of the appointments list (DOCTOR / PATIENT callers are scoped to themselves by the backend). */
@Injectable()
export class AppointmentsResourceService {
  private readonly _appointmentsApi = inject(AppointmentsApi);

  public filters = signal<AppointmentFilters>(getDefaultAppointmentFilters());

  private _appointmentsResource = rxResource({
    request: () => this.filters(),
    loader: ({ request }) => this._appointmentsApi.list(request),
  });

  public appointments = computed<AppointmentRow[]>(() =>
    (this._appointmentsResource.value()?.content ?? []).map((appointment) => ({
      ...appointment,
      dayLabel: formatDayLabel(datePart(appointment.startTime)),
      timeLabel: `${formatTime(appointment.startTime)} – ${formatTime(appointment.endTime)}`,
    })),
  );
  public pagination = computed(() => this._appointmentsResource.value()?.page ?? null);
  public status = computed<AppointmentStatusFilter>(() => this.filters().status ?? 'ALL');
  public isLoading = this._appointmentsResource.isLoading;
  public isError = computed(() => !!this._appointmentsResource.error());
  public errorMessage = computed(() => {
    const error = this._appointmentsResource.error();
    return error ? apiErrorMessage(error, 'No se pudieron cargar las citas') : '';
  });
  public reloadAppointments = () => this._appointmentsResource.reload();

  public setStatus = (status: AppointmentStatusFilter) =>
    this.filters.update((current) => ({
      ...current,
      page: 0,
      status: status === 'ALL' ? undefined : status,
    }));

  public setPage = (page: number) => this.filters.update((current) => ({ ...current, page }));

  public resetResource = () => this.filters.set(getDefaultAppointmentFilters());
}
