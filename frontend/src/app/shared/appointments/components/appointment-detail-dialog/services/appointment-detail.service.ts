import { Injectable, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA } from '@angular/cdk/dialog';
import { rxResource } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, finalize, tap } from 'rxjs';
import { AppointmentsApi } from '../../../../../core/api/appointments.api';
import { canCancelAppointment, canConfirmAppointment } from '../../../../../core/auth/permissions';
import { resourceError } from '../../../../../core/http/api-error';
import { AppointmentResponse } from '../../../../../core/models/api.models';
import { datePart, formatLongDate } from '../../../../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../../services/appointment-actions.service';

@Injectable()
export class AppointmentDetailService {
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly id = inject<{ id: number }>(DIALOG_DATA).id;

  private readonly resource = rxResource({ loader: () => this.api.get(this.id) });
  private readonly _busy = signal(false);
  private _changed = false;

  readonly busy = this._busy.asReadonly();
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, 'No se pudo cargar la cita');
  readonly appointment = computed(() =>
    this.resource.hasValue() ? this.resource.value() : undefined,
  );
  readonly dateLabel = computed(() => {
    const appointment = this.appointment();
    return appointment ? formatLongDate(datePart(appointment.startTime)) : '';
  });
  readonly canConfirm = computed(() => {
    const appointment = this.appointment();
    return !!appointment && canConfirmAppointment(appointment.status);
  });
  readonly canCancel = computed(() => {
    const appointment = this.appointment();
    return !!appointment && canCancelAppointment(appointment.status);
  });

  get changed(): boolean {
    return this._changed;
  }

  reload(): void {
    this.resource.reload();
  }

  confirm(): Observable<AppointmentResponse> {
    return this.apply((a) => this.actions.confirm(a.id, a.patientName, a.startTime));
  }

  cancel(): Observable<AppointmentResponse> {
    return this.apply((a) => this.actions.cancel(a.id, a.patientName, a.startTime));
  }

  private apply(
    action: (appointment: AppointmentResponse) => Observable<AppointmentResponse>,
  ): Observable<AppointmentResponse> {
    const appointment = this.appointment();
    if (!appointment || this.busy()) return EMPTY;
    this._busy.set(true);
    return action(appointment).pipe(
      tap((updated) => {
        this._changed = true;
        this.resource.set(updated);
      }),
      finalize(() => this._busy.set(false)),
    );
  }
}
