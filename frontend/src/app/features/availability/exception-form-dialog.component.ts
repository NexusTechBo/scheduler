import { Component, inject, signal } from '@angular/core';
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
import { apiErrorMessage } from '../../core/http/api-error';
import { toIso } from '../../core/utils/calendar.util';
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

/** Blocks a whole day (vacation, congress…) or a time range of a specific date. */
@Component({
  selector: 'app-exception-form-dialog',
  imports: [DialogFrameComponent, DateRangePickerComponent, ReactiveFormsModule],
  templateUrl: './exception-form-dialog.component.html',
})
export class ExceptionFormDialogComponent {
  private readonly api = inject(AgendaApi);
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);

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

  protected close(): void {
    this.ref.close(false);
  }

  protected save(): void {
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
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.ref.close(true),
        error: (error) => this.error.set(apiErrorMessage(error, 'No se pudo guardar el bloqueo')),
      });
  }
}
