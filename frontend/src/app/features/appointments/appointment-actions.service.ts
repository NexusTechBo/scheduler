import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { EMPTY, Observable, catchError, filter, map, switchMap, tap } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { AppointmentResponse, AppointmentStatus } from '../../core/models/api.models';
import { ROLES } from '../../core/navigation/navigation';
import { ConfirmService } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NotificationService } from '../../shared/services/notification.service';
import {
  AppointmentDetailDialogComponent,
  AppointmentDetailData,
} from './appointment-detail-dialog.component';
import {
  CancelAppointmentData,
  CancelAppointmentDialogComponent,
  CancelAppointmentResult,
} from './cancel-appointment-dialog.component';

/**
 * Confirm / cancel / detail flows shared by the board, the calendar and the appointments list.
 * Every action asks for confirmation, notifies the result and only emits on success.
 */
@Injectable({ providedIn: 'root' })
export class AppointmentActionsService {
  private readonly api = inject(AppointmentsApi);
  private readonly auth = inject(AuthService);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);

  /** Backend: PATCH confirm/cancel are DOCTOR / ASSISTANT only. */
  canManage(): boolean {
    return this.auth.hasRole(...ROLES.clinical);
  }

  canConfirm(status: AppointmentStatus): boolean {
    return this.canManage() && status === 'PENDING';
  }

  canCancel(status: AppointmentStatus): boolean {
    return this.canManage() && status !== 'CANCELLED';
  }

  confirm(id: number, patientName: string): Observable<AppointmentResponse> {
    return this.confirmService
      .ask({
        title: 'Confirmar cita',
        message: `¿Confirmar la cita de ${patientName}?`,
        confirmLabel: 'Confirmar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.run(this.api.confirm(id), 'Cita confirmada')),
      );
  }

  /** Asks for an optional reason (quick options or free text) that the patient will see. */
  cancel(id: number, patientName: string): Observable<AppointmentResponse> {
    return this.dialog
      .open<CancelAppointmentResult, CancelAppointmentData>(CancelAppointmentDialogComponent, {
        data: { patientName },
        backdropClass: 'glass-backdrop',
      })
      .closed.pipe(
        filter((result): result is CancelAppointmentResult => !!result),
        switchMap(({ reason }) => this.run(this.api.cancel(id, reason), 'Cita cancelada')),
      );
  }

  /** Emits `true` when the appointment changed inside the dialog, so callers can reload. */
  openDetail(id: number): Observable<boolean> {
    return this.dialog
      .open<boolean, AppointmentDetailData>(AppointmentDetailDialogComponent, {
        data: { id },
        backdropClass: 'glass-backdrop',
        // Footer starts with "Cancelar cita"; don't land focus on a destructive action.
        autoFocus: 'first-heading',
      })
      .closed.pipe(map((changed) => changed === true));
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
