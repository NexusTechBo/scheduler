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

@Injectable({ providedIn: 'root' })
export class AppointmentActionsService {
  private readonly api = inject(AppointmentsApi);
  private readonly auth = inject(AuthService);
  private readonly confirmService = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);

  canManage(): boolean {
    return this.auth.hasRole(...ROLES.staff);
  }

  canConfirm(status: AppointmentStatus): boolean {
    return this.canManage() && status === 'PENDING';
  }

  canCancel(status: AppointmentStatus): boolean {
    return this.auth.hasRole(...ROLES.everyone) && status !== 'CANCELLED';
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

  cancel(id: number, patientName: string, doctorName: string): Observable<AppointmentResponse> {
    return this.dialog
      .open<CancelAppointmentResult, CancelAppointmentData>(CancelAppointmentDialogComponent, {
        data: { patientName, doctorName, asPatient: this.auth.hasRole('PATIENT') },
        backdropClass: 'glass-backdrop',
      })
      .closed.pipe(
        filter((result): result is CancelAppointmentResult => !!result),
        switchMap(({ reason }) => this.run(this.api.cancel(id, reason), 'Cita cancelada')),
      );
  }

  openDetail(id: number): Observable<boolean> {
    return this.dialog
      .open<boolean, AppointmentDetailData>(AppointmentDetailDialogComponent, {
        data: { id },
        backdropClass: 'glass-backdrop',
        autoFocus: 'first-heading',
        disableClose: true,
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
