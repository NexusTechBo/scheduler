import { Component, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { FormsModule } from '@angular/forms';
import { DateRangePickerComponent } from '../../shared/ui/date-range-picker/date-range-picker.component';
import { filter, switchMap } from 'rxjs';
import { AgendaApi } from '../../core/api/agenda.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { Availability, ScheduleException } from '../../core/models/api.models';
import { formatLongDate, formatTime } from '../../core/utils/calendar.util';
import { NotificationService } from '../../shared/services/notification.service';
import { ConfirmService } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { AvailabilityFormDialogComponent } from './availability-form-dialog.component';
import { ExceptionFormDialogComponent } from './exception-form-dialog.component';
import { AvailabilityResourceService } from './services/availability-resource.service';

/** DOCTOR: weekly availability blocks, schedule exceptions and a preview of the resulting free slots. */
@Component({
  selector: 'app-availability-page',
  imports: [
    FormsModule,
    DateRangePickerComponent,
    PageHeaderComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [AvailabilityResourceService],
  templateUrl: './availability-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class AvailabilityPageComponent {
  private readonly api = inject(AgendaApi);
  private readonly dialog = inject(Dialog);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly resource = inject(AvailabilityResourceService);
  protected readonly formatTime = formatTime;
  protected readonly formatLongDate = formatLongDate;

  protected addAvailability(): void {
    this.dialog
      .open<boolean>(AvailabilityFormDialogComponent, { backdropClass: 'glass-backdrop' })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success('Horario agregado');
        this.resource.reloadAvailability();
      });
  }

  protected removeAvailability(block: Availability, dayLabel: string): void {
    this.confirm
      .ask({
        title: 'Quitar horario',
        message: `¿Quitar el bloque del ${dayLabel.toLowerCase()} de ${formatTime(block.startTime)} a ${formatTime(block.endTime)}? Las citas ya agendadas se mantienen.`,
        confirmLabel: 'Quitar',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.removeAvailability(block.id)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Horario quitado');
          this.resource.reloadAvailability();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error)),
      });
  }

  protected addException(): void {
    this.dialog
      .open<boolean>(ExceptionFormDialogComponent, { backdropClass: 'glass-backdrop' })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success('Agenda bloqueada');
        this.resource.reloadAvailability();
      });
  }

  protected removeException(exception: ScheduleException): void {
    this.confirm
      .ask({
        title: 'Quitar bloqueo',
        message: `¿Habilitar nuevamente el ${formatLongDate(exception.date).toLowerCase()}?`,
        confirmLabel: 'Quitar',
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.removeException(exception.id)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Bloqueo quitado');
          this.resource.reloadAvailability();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error)),
      });
  }
}
