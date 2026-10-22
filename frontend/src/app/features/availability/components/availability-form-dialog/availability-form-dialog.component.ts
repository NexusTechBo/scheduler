import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { DialogRef } from '@angular/cdk/dialog';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize, map } from 'rxjs';
import { DayOfWeek } from '../../../../core/models/api.models';
import { addMinutesToTime } from '../../../../core/utils/calendar.util';
import { DAYS_OF_WEEK } from '../../../../core/utils/labels.util';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { SelectComponent, SelectOption } from '../../../../shared/ui/forms/select/select.component';
import { TimePickerComponent } from '../../../../shared/ui/forms/time-picker/time-picker.component';
import { AvailabilityService } from '../../services/availability.service';
import { AlertComponent } from '../../../../shared/ui/alert/alert.component';

function endAfterStart(control: AbstractControl): ValidationErrors | null {
  const startTime = control.parent?.get('startTime')?.value as string | undefined;
  return startTime && control.value && control.value <= startTime ? { endBeforeStart: true } : null;
}

@Component({
  selector: 'app-availability-form-dialog',
  imports: [
    AlertComponent,
    DialogFrameComponent,
    ReactiveFormsModule,
    FormFieldComponent,
    SelectComponent,
    TimePickerComponent,
  ],
  templateUrl: './availability-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvailabilityFormDialogComponent {
  private readonly availability = inject(AvailabilityService);
  protected readonly ref = inject(DialogRef);

  protected readonly days = DAYS_OF_WEEK;
  protected readonly durationOptions: SelectOption<number>[] = [15, 20, 30, 45, 60].map(
    (minutes) => ({ value: minutes, label: `${minutes} minutos` }),
  );
  protected readonly selectedDays = signal<Set<DayOfWeek>>(new Set(['MONDAY']));
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    startTime: ['08:00', Validators.required],
    endTime: ['12:00', [Validators.required, endAfterStart]],
    slotDurationMinutes: [30, [Validators.required, Validators.min(5)]],
  });
  protected readonly endErrors = {
    endBeforeStart: 'La hora de fin debe ser posterior a la de inicio',
  };

  private readonly startTime = toSignal(this.form.controls.startTime.valueChanges, {
    initialValue: this.form.controls.startTime.value,
  });
  protected readonly endMin = computed(() => addMinutesToTime(this.startTime(), 30));
  protected readonly rangeInvalid = toSignal(
    this.form.controls.endTime.events.pipe(
      map(() => {
        const end = this.form.controls.endTime;
        return end.touched && end.hasError('endBeforeStart');
      }),
    ),
    { initialValue: false },
  );

  constructor() {
    this.form.controls.startTime.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      const end = this.form.controls.endTime;
      end.updateValueAndValidity();
      end.markAsTouched();
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.error.set(null));
  }

  protected toggleDay(day: DayOfWeek): void {
    this.error.set(null);
    this.selectedDays.update((days) => {
      const next = new Set(days);
      if (next.has(day)) next.delete(day);
      else next.add(day);
      return next;
    });
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid || !this.selectedDays().size) {
      this.form.markAllAsTouched();
      return;
    }
    const days = this.days.map((d) => d.value).filter((day) => this.selectedDays().has(day));
    this.saving.set(true);
    this.availability
      .addAvailability(days, this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe(({ saved, failed }) => {
        if (!failed.length) {
          this.ref.close();
          return;
        }
        this.selectedDays.set(new Set(failed.map((f) => f.day)));
        const label = (day: DayOfWeek) => this.days.find((d) => d.value === day)!.label;
        this.error.set(
          (saved.length ? `Se guardó: ${saved.map(label).join(', ')}. ` : '') +
            `No se pudo guardar: ${failed.map((f) => `${label(f.day)} (${f.message})`).join('; ')}`,
        );
      });
  }
}
