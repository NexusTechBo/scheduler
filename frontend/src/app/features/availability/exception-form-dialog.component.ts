import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DialogRef } from '@angular/cdk/dialog';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { AgendaApi } from '../../core/api/agenda.api';
import { activeAppointmentsConflict, apiErrorMessage } from '../../core/http/api-error';
import { toIso } from '../../core/utils/calendar.util';
import { ConfirmService } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { DateRangePickerComponent } from '../../shared/ui/date-range-picker/date-range-picker.component';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';

function partialRangeValid(group: AbstractControl): ValidationErrors | null {
  const { isFullDayBlock, startTime, endTime } = group.value as {
    isFullDayBlock: boolean;
    startTime: string;
    endTime: string;
  };
  if (isFullDayBlock) return null;
  if (!startTime || !endTime) return { rangeRequired: true };
  return endTime <= startTime ? { endBeforeStart: true } : null;
}

@Component({
  selector: 'app-exception-form-dialog',
  imports: [DialogFrameComponent, DateRangePickerComponent, ReactiveFormsModule],
  templateUrl: './exception-form-dialog.component.html',
})
export class ExceptionFormDialogComponent {
  private readonly api = inject(AgendaApi);
  private readonly ref = inject<DialogRef<string>>(DialogRef);
  private readonly confirm = inject(ConfirmService);
  private readonly submitButton = viewChild.required<ElementRef<HTMLButtonElement>>('submitButton');

  protected readonly today = toIso(new Date());
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      date: [this.today, Validators.required],
      isFullDayBlock: [true],
      startTime: ['09:00'],
      endTime: ['10:00'],
      reason: [''],
    },
    { validators: partialRangeValid },
  );

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.error.set(null));
  }

  protected close(): void {
    this.ref.close();
  }

  protected save(): void {
    this.submit(false, 'Agenda bloqueada');
  }

  private submit(cancelAppointments: boolean, successMessage: string): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .addException({
        date: value.date,
        isFullDayBlock: value.isFullDayBlock,
        startTime: value.isFullDayBlock ? null : value.startTime,
        endTime: value.isFullDayBlock ? null : value.endTime,
        reason: value.reason.trim() || null,
        cancelAppointments,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.ref.close(successMessage),
        error: (error) => {
          const count = activeAppointmentsConflict(error);
          if (count) this.confirmCancel(count);
          else this.error.set(apiErrorMessage(error, 'No se pudo guardar el bloqueo'));
        },
      });
  }

  private confirmCancel(count: number): void {
    const one = count === 1;
    this.confirm
      .ask({
        title: 'Cancelar citas',
        message: one
          ? 'Hay 1 cita activa en este horario. Si bloqueas, se cancelará y se notificará al paciente.'
          : `Hay ${count} citas activas en este horario. Si bloqueas, se cancelarán y se notificará a los pacientes.`,
        confirmLabel: 'Bloquear y cancelar citas',
        danger: true,
      })
      .subscribe((confirmed) => {
        if (!confirmed) {
          this.submitButton().nativeElement.focus();
          return;
        }
        this.submit(
          true,
          one
            ? 'Agenda bloqueada. Se canceló la cita y se notificó al paciente'
            : 'Agenda bloqueada. Se cancelaron las citas y se notificó a los pacientes',
        );
      });
  }
}
