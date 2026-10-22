import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  catchError,
  concatMap,
  finalize,
  from,
  map,
  of,
  switchMap,
  tap,
  throwError,
  toArray,
} from 'rxjs';
import { AgendaApi } from '../../../core/api/agenda.api';
import { AuthService } from '../../../core/auth/auth.service';
import {
  activeAppointmentsConflict,
  apiErrorMessage,
  resourceError,
} from '../../../core/http/api-error';
import {
  Availability,
  AvailabilityRequest,
  DayOfWeek,
  ScheduleException,
  ScheduleExceptionRequest,
} from '../../../core/models/api.models';
import { formatLongDate, formatTime, toIso } from '../../../core/utils/calendar.util';
import { DAYS_OF_WEEK } from '../../../core/utils/labels.util';
import { NotificationService } from '../../../shared/services/notification.service';
import { ConfirmService } from '../../../shared/ui/confirm-dialog/services/confirm.service';

type AvailabilityBlock = Omit<AvailabilityRequest, 'dayOfWeek'>;

interface AddAvailabilityResult {
  saved: DayOfWeek[];
  failed: { day: DayOfWeek; message: string }[];
}

@Injectable()
export class AvailabilityService {
  private readonly api = inject(AgendaApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly doctorId = inject(AuthService).userId()!;

  readonly today = toIso(new Date());
  readonly previewDate = signal(this.today);

  private readonly availabilityResource = rxResource({ loader: () => this.api.availabilities() });
  private readonly exceptionsResource = rxResource({
    loader: () => this.api.exceptions(this.today),
  });
  private readonly previewResource = rxResource({
    request: () => this.previewDate(),
    loader: ({ request }) =>
      this.api
        .availableSlots(this.doctorId, request)
        .pipe(map((response) => response.availableSlots.map((slot) => formatTime(slot.startTime)))),
  });

  readonly week = computed(() => {
    const blocks = this.availabilityResource.value() ?? [];
    return DAYS_OF_WEEK.map((day) => ({
      ...day,
      blocks: blocks
        .filter((block) => block.dayOfWeek === day.value)
        .sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }));
  });
  readonly exceptions = computed(() => this.exceptionsResource.value() ?? []);
  readonly previewSlots = computed(() => this.previewResource.value() ?? []);

  readonly isWeekLoading = this.availabilityResource.isLoading;
  readonly isExceptionsLoading = this.exceptionsResource.isLoading;
  readonly isPreviewLoading = this.previewResource.isLoading;
  readonly weekError = resourceError(this.availabilityResource, 'No se pudo cargar tu horario');
  readonly exceptionsError = resourceError(
    this.exceptionsResource,
    'No se pudieron cargar los bloqueos',
  );

  reload(): void {
    this.availabilityResource.reload();
    this.exceptionsResource.reload();
    this.previewResource.reload();
  }

  setPreviewDate(iso: string): void {
    if (iso) this.previewDate.set(iso);
  }

  addAvailability(days: DayOfWeek[], block: AvailabilityBlock): Observable<AddAvailabilityResult> {
    return from(days).pipe(
      concatMap((day) =>
        this.api.addAvailability({ dayOfWeek: day, ...block }).pipe(
          map(() => ({ day, message: null })),
          catchError((error) =>
            of({ day, message: apiErrorMessage(error, 'No se pudo guardar el horario') }),
          ),
        ),
      ),
      toArray(),
      map((results) => ({
        saved: results.filter((r) => r.message === null).map((r) => r.day),
        failed: results.flatMap(({ day, message }) => (message === null ? [] : [{ day, message }])),
      })),
      tap(({ saved }) => {
        if (saved.length) this.notifications.success('Horario agregado');
      }),
      finalize(() => this.reload()),
    );
  }

  removeAvailability(block: Availability, dayLabel: string): void {
    this.confirm
      .confirmThen(
        {
          title: 'Quitar horario',
          message: `¿Quitar el bloque del ${dayLabel.toLowerCase()} de ${formatTime(block.startTime)} a ${formatTime(block.endTime)}? Las citas ya agendadas se mantienen.`,
          confirmLabel: 'Quitar',
          danger: true,
        },
        () => this.api.removeAvailability(block.id),
        'Horario quitado',
        'No se pudo quitar el horario',
      )
      .subscribe(() => this.reload());
  }

  addException(request: Omit<ScheduleExceptionRequest, 'cancelAppointments'>): Observable<void> {
    return this.api.addException({ ...request, cancelAppointments: false }).pipe(
      map(() => 'Agenda bloqueada'),
      catchError((error) => {
        const count = activeAppointmentsConflict(error);
        if (count === null) return throwError(() => error);
        return this.confirmCancelAppointments(count).pipe(
          switchMap(() => this.api.addException({ ...request, cancelAppointments: true })),
          map(() =>
            count === 1
              ? 'Agenda bloqueada. Se canceló la cita y se notificó al paciente'
              : 'Agenda bloqueada. Se cancelaron las citas y se notificó a los pacientes',
          ),
        );
      }),
      tap((message) => {
        this.notifications.success(message);
        this.reload();
      }),
      map(() => undefined),
    );
  }

  removeException(exception: ScheduleException): void {
    this.confirm
      .confirmThen(
        {
          title: 'Quitar bloqueo',
          message: `¿Habilitar nuevamente el ${formatLongDate(exception.date).toLowerCase()}?`,
          confirmLabel: 'Quitar',
        },
        () => this.api.removeException(exception.id),
        'Bloqueo quitado',
        'No se pudo quitar el bloqueo',
      )
      .subscribe(() => this.reload());
  }

  private confirmCancelAppointments(count: number): Observable<true> {
    const message =
      count === 1
        ? 'Hay 1 cita activa en este horario. Si bloqueas, se cancelará y se notificará al paciente.'
        : count > 1
          ? `Hay ${count} citas activas en este horario. Si bloqueas, se cancelarán y se notificará a los pacientes.`
          : 'Hay citas activas en este horario. Si bloqueas, se cancelarán y se notificará a los pacientes.';
    return this.confirm
      .ask({
        title: 'Cancelar citas',
        message,
        confirmLabel: 'Bloquear y cancelar citas',
        danger: true,
      })
      .pipe(switchMap((confirmed) => (confirmed ? [true as const] : EMPTY)));
  }
}
