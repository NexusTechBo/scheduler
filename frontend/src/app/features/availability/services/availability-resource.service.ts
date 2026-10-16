import { computed, inject, Injectable, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { AgendaApi } from '../../../core/api/agenda.api';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { ScheduleException } from '../../../core/models/api.models';
import { formatTime, toIso } from '../../../core/utils/calendar.util';
import { DAYS_OF_WEEK } from '../../../core/utils/labels.util';

@Injectable()
export class AvailabilityResourceService {
  private readonly _agendaApi = inject(AgendaApi);
  private readonly _auth = inject(AuthService);

  private readonly _doctorId = this._auth.userId()!;
  public readonly today = toIso(new Date());

  public previewDate = signal(this.today);

  private _availabilityResource = rxResource({
    loader: () => this._agendaApi.availabilities(),
  });
  private _exceptionsResource = rxResource({
    loader: () => this._agendaApi.exceptions(this.today),
  });
  private _previewResource = rxResource({
    request: () => this.previewDate(),
    loader: ({ request }) =>
      this._agendaApi
        .availableSlots(this._doctorId, request)
        .pipe(map((response) => response.availableSlots.map((slot) => formatTime(slot.startTime)))),
  });

  public week = computed(() => {
    const blocks = this._availabilityResource.value() ?? [];
    return DAYS_OF_WEEK.map((day) => ({
      ...day,
      blocks: blocks
        .filter((block) => block.dayOfWeek === day.value)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }));
  });
  public exceptions = computed(
    () => this._exceptionsResource.value() ?? ([] as ScheduleException[]),
  );
  public previewSlots = computed(() => this._previewResource.value() ?? ([] as string[]));

  public isWeekLoading = this._availabilityResource.isLoading;
  public isExceptionsLoading = this._exceptionsResource.isLoading;
  public isPreviewLoading = this._previewResource.isLoading;
  public isWeekError = computed(() => !!this._availabilityResource.error());
  public weekErrorMessage = computed(() => {
    const error = this._availabilityResource.error();
    return error ? apiErrorMessage(error, 'No se pudo cargar tu horario') : '';
  });
  public isExceptionsError = computed(() => !!this._exceptionsResource.error());
  public exceptionsErrorMessage = computed(() => {
    const error = this._exceptionsResource.error();
    return error ? apiErrorMessage(error, 'No se pudieron cargar los bloqueos') : '';
  });
  public reloadExceptions = () => this._exceptionsResource.reload();

  public setPreviewDate = (iso: string) => iso && this.previewDate.set(iso);

  public reloadAvailability = () => {
    this._availabilityResource.reload();
    this._exceptionsResource.reload();
    this._previewResource.reload();
  };
}
