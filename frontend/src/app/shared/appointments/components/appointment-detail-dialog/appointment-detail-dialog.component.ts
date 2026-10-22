import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, filter, merge } from 'rxjs';
import { AppointmentsApi } from '../../../../core/api/appointments.api';
import { AuthService } from '../../../../core/auth/auth.service';
import { canCancelAppointment, canConfirmAppointment } from '../../../../core/auth/permissions';
import { AppointmentResponse } from '../../../../core/models/api.models';
import { datePart, formatLongDate } from '../../../../core/utils/calendar.util';
import { SpecialtyLabelPipe } from '../../../pipes/specialty-label.pipe';
import { TimeRangePipe } from '../../../pipes/time-range.pipe';
import { DialogFrameComponent } from '../../../ui/dialog-frame/dialog-frame.component';
import { SkeletonComponent } from '../../../ui/skeleton/skeleton.component';
import { AlertComponent } from '../../../ui/alert/alert.component';
import { StatusBadgeComponent } from '../../../ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../../ui/ui-icon/ui-icon.component';
import { AppointmentActionsService } from '../../services/appointment-actions.service';

export interface AppointmentDetailData {
  id: number;
}

@Component({
  selector: 'app-appointment-detail-dialog',
  imports: [
    AlertComponent,
    DialogFrameComponent,
    SkeletonComponent,
    StatusBadgeComponent,
    UiIconComponent,
    TimeRangePipe,
    SpecialtyLabelPipe,
  ],
  templateUrl: './appointment-detail-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentDetailDialogComponent {
  private readonly data = inject<AppointmentDetailData>(DIALOG_DATA);
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly api = inject(AppointmentsApi);
  private readonly auth = inject(AuthService);
  private readonly actions = inject(AppointmentActionsService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  private changed = false;
  protected readonly busy = signal(false);

  protected readonly resource = rxResource({ loader: () => this.api.get(this.data.id) });
  protected readonly appointment = computed(() =>
    this.resource.hasValue() ? this.resource.value() : undefined,
  );
  protected readonly dateLabel = computed(() => {
    const appointment = this.appointment();
    return appointment ? formatLongDate(datePart(appointment.startTime)) : '';
  });
  protected readonly canConfirm = computed(() => {
    const appointment = this.appointment();
    return !!appointment && canConfirmAppointment(this.auth.role(), appointment.status);
  });
  protected readonly canCancel = computed(() => {
    const appointment = this.appointment();
    return !!appointment && canCancelAppointment(this.auth.role(), appointment.status);
  });

  constructor() {
    merge(
      this.ref.backdropClick.pipe(filter((event) => event.detail <= 1)),
      this.ref.keydownEvents.pipe(filter((event) => event.key === 'Escape')),
    ).subscribe(() => this.close());
  }

  protected confirm(appointment: AppointmentResponse, event: Event): void {
    this.apply(
      this.actions.confirm(appointment.id, appointment.patientName, appointment.startTime),
      event,
    );
  }

  protected cancel(appointment: AppointmentResponse, event: Event): void {
    this.apply(
      this.actions.cancel(appointment.id, appointment.patientName, appointment.startTime),
      event,
    );
  }

  protected close(): void {
    this.ref.close(this.changed);
  }

  private apply(action: Observable<AppointmentResponse>, event: Event): void {
    const trigger = event.currentTarget as HTMLElement;
    if (this.busy()) return;
    this.busy.set(true);
    action.subscribe({
      next: (updated) => {
        this.changed = true;
        this.resource.set(updated);
      },
      complete: () => {
        this.busy.set(false);
        afterNextRender(
          () =>
            (trigger.isConnected
              ? trigger
              : this.host.nativeElement.querySelector<HTMLElement>('h2')
            )?.focus(),
          { injector: this.injector },
        );
      },
    });
  }
}
