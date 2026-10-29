import { Dialog } from '@angular/cdk/dialog';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, catchError, filter, map, switchMap, tap } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { AppointmentResponse } from '../../core/models/api.models';
import { datePart, formatDayLabel, formatTime } from '../../core/utils/calendar.util';
import { ConfirmService } from './confirm.service';
import { NotificationService } from './notification.service';
import { labelledByFrame } from '../ui/dialog-frame/dialog-frame.component';
import {
  AppointmentDetailData,
  AppointmentDetailDialogComponent,
} from '../components/appointment-detail-dialog/appointment-detail-dialog.component';
import {
  CancelAppointmentData,
  CancelAppointmentDialogComponent,
  CancelAppointmentResult,
} from '../components/cancel-appointment-dialog/cancel-appointment-dialog.component';

@Injectable({ providedIn: 'root' })
export class AppointmentActionsService {
  private readonly api = inject(AppointmentsApi);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);

  confirm(id: number, patientName: string, startTime: string): Observable<AppointmentResponse> {
    return this.confirmService
      .ask({
        title: 'Confirmar cita',
        message: `¿Confirmar la cita de ${patientName} del ${dayTime(startTime)}?`,
        confirmLabel: 'Confirmar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.run(this.api.confirm(id), 'Cita confirmada')),
      );
  }

  cancel(id: number, patientName: string, startTime: string): Observable<AppointmentResponse> {
    return this.dialog
      .open<CancelAppointmentResult, CancelAppointmentData>(CancelAppointmentDialogComponent, {
        ...labelledByFrame(),
        data: { patientName, when: dayTime(startTime) },
      })
      .closed.pipe(
        filter((result): result is CancelAppointmentResult => !!result),
        switchMap(({ reason }) => this.run(this.api.cancel(id, { reason }), 'Cita cancelada')),
      );
  }

  openDetail(id: number): Observable<void> {
    return this.dialog
      .open<boolean, AppointmentDetailData>(AppointmentDetailDialogComponent, {
        ...labelledByFrame(),
        data: { id },
        autoFocus: 'first-heading',
        disableClose: true,
      })
      .closed.pipe(
        filter((changed) => changed === true),
        map(() => undefined),
      );
  }

  private run(
    request: Observable<AppointmentResponse>,
    successMessage: string,
  ): Observable<AppointmentResponse> {
    return request.pipe(
      tap(() => this.notifications.success(successMessage)),
      catchError((error) => {
        this.notifications.error(apiErrorMessage(error));
        return EMPTY;
      }),
    );
  }
}

function dayTime(startTime: string): string {
  return `${formatDayLabel(datePart(startTime)).toLowerCase()} ${formatTime(startTime)}`;
}
