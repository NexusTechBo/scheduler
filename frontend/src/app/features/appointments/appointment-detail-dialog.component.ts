import { Component, computed, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { AppointmentResponse } from '../../core/models/api.models';
import { datePart, formatLongDate, formatTime } from '../../core/utils/calendar.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { StatusBadgeComponent } from '../../shared/ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { AppointmentActionsService } from './appointment-actions.service';

export interface AppointmentDetailData {
  id: number;
}

@Component({
  selector: 'app-appointment-detail-dialog',
  imports: [DialogFrameComponent, StatusBadgeComponent, UiIconComponent],
  templateUrl: './appointment-detail-dialog.component.html',
})
export class AppointmentDetailDialogComponent {
  private readonly data = inject<AppointmentDetailData>(DIALOG_DATA);
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);

  private changed = false;
  protected readonly busy = signal(false);

  protected readonly resource = rxResource({ loader: () => this.api.get(this.data.id) });
  protected readonly appointment = this.resource.value;
  protected readonly dateLabel = computed(() => {
    const appointment = this.appointment();
    return appointment ? formatLongDate(datePart(appointment.startTime)) : '';
  });
  protected readonly timeLabel = computed(() => {
    const appointment = this.appointment();
    return appointment
      ? `${formatTime(appointment.startTime)} – ${formatTime(appointment.endTime)}`
      : '';
  });
  protected readonly canConfirm = computed(() => {
    const appointment = this.appointment();
    return !!appointment && this.actions.canConfirm(appointment.status);
  });
  protected readonly canCancel = computed(() => {
    const appointment = this.appointment();
    return !!appointment && this.actions.canCancel(appointment.status);
  });

  protected confirm(appointment: AppointmentResponse): void {
    this.apply(this.actions.confirm(appointment.id, appointment.patientName));
  }

  protected cancel(appointment: AppointmentResponse): void {
    this.apply(this.actions.cancel(appointment.id, appointment.patientName));
  }

  protected close(): void {
    this.ref.close(this.changed);
  }

  private apply(action: Observable<AppointmentResponse>): void {
    if (this.busy()) return;
    this.busy.set(true);
    action.subscribe({
      next: (updated) => {
        this.changed = true;
        this.resource.set(updated);
      },
      complete: () => this.busy.set(false),
    });
  }
}
