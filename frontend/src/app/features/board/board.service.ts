import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, debounceTime, merge, skip } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { BoardSocket } from '../../core/api/board.socket';
import { resourceError } from '../../core/http/api-error';
import { AppointmentStatus, AppointmentSummaryItem } from '../../core/models/api.models';
import { addDays, toIso, toLocalDateTime } from '../../core/utils/calendar.util';
import {
  APPOINTMENT_STATUSES,
  STATUS_PLURAL_LABEL,
  STATUS_STYLE,
} from '../../core/utils/labels.util';
import { AppointmentActionsService } from '../../shared/services/appointment-actions.service';
import { NotificationService } from '../../shared/services/notification.service';
import { DateRange } from '../../shared/ui/forms/date-range-picker/date-range-picker.component';
import { BoardColumn } from './board.model';

@Injectable()
export class BoardService {
  private readonly api = inject(AppointmentsApi);
  private readonly actions = inject(AppointmentActionsService);
  private readonly notifications = inject(NotificationService);

  private readonly _range = signal<DateRange>({
    from: toIso(new Date()),
    to: toIso(addDays(new Date(), 6)),
  });
  readonly range = this._range.asReadonly();
  readonly showDates = computed(() => this.range().from !== this.range().to);

  private readonly resource = rxResource({
    request: () => this.range(),
    loader: ({ request: { from, to } }) => this.api.board({ from, to }),
  });

  readonly isLoading = this.resource.isLoading;
  readonly hasData = computed(() => this.resource.hasValue());
  readonly errorMessage = resourceError(this.resource, 'No se pudo cargar el tablero');
  readonly columns = computed<BoardColumn[]>(() => {
    const board = this.resource.hasValue() ? this.resource.value() : undefined;
    return APPOINTMENT_STATUSES.map((status) => ({
      id: status,
      title: STATUS_PLURAL_LABEL[status],
      accentClass: STATUS_STYLE[status].dot,
      appointments: [...(board?.[status] ?? [])],
    }));
  });

  private readonly socket = inject(BoardSocket);

  constructor() {
    merge(this.socket.events$, this.socket.connected$.pipe(skip(1)))
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => this.resource.reload());
  }

  reload(): void {
    this.resource.reload();
  }

  setRange(range: DateRange): void {
    this._range.set(range);
  }

  open(appointment: AppointmentSummaryItem): void {
    this.actions.openDetail(appointment.id).subscribe(() => this.refreshAfterAction());
  }

  move(appointment: AppointmentSummaryItem, target: AppointmentStatus): void {
    const { id, clientName } = appointment;
    const start = toLocalDateTime(appointment.appointmentDate, appointment.startTime);
    let action: Observable<unknown> | null = null;
    if (target === 'CONFIRMED' && appointment.status === 'PENDING')
      action = this.actions.confirm(id, clientName, start);
    if (target === 'CANCELLED') action = this.actions.cancel(id, clientName, start);

    if (!action) {
      this.notifications.error(
        target === 'PENDING'
          ? 'Una cita no puede volver a pendiente'
          : 'Una cita cancelada no puede reactivarse',
      );
      return;
    }
    action.subscribe(() => this.refreshAfterAction());
  }

  private refreshAfterAction(): void {
    if (!this.socket.isConnected()) this.reload();
  }
}
