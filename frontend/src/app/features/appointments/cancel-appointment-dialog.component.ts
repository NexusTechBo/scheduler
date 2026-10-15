import { Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';

export interface CancelAppointmentData {
  patientName: string;
}

export interface CancelAppointmentResult {
  /** null when no message was left. */
  reason: string | null;
}

const REASON_MAX_LENGTH = 500;

const QUICK_REASONS = [
  'El doctor no estará disponible',
  'Solicitado por el paciente',
  'Emergencia médica',
  'Se reprogramará la cita',
  'Cambio de horario de la clínica',
];

/** Asks for confirmation and an optional reason (shown to the patient). Closes with `undefined` when dismissed. */
@Component({
  selector: 'app-cancel-appointment-dialog',
  imports: [DialogFrameComponent, UiIconComponent],
  template: `
    <app-dialog-frame title="Cancelar cita" (closed)="ref.close()">
      <p class="text-[15px] leading-relaxed text-gray-600">
        ¿Cancelar la cita de <span class="font-bold text-gray-900">{{ data.patientName }}</span>? El
        horario quedará libre nuevamente.
      </p>

      <p class="field-label mt-5">Motivos rápidos</p>
      <div class="flex flex-wrap gap-2">
        @for (option of quickReasons; track option) {
          <button
            type="button"
            class="chip cursor-pointer px-3 py-1.5 text-sm font-semibold ring-1 transition-colors"
            [class]="
              reason() === option
                ? 'bg-primary/10 text-primary ring-primary/40'
                : 'bg-white/60 text-gray-600 ring-gray-900/10 hover:bg-white hover:text-gray-800'
            "
            [attr.aria-pressed]="reason() === option"
            (click)="pick(option)"
          >
            @if (reason() === option) {
              <app-ui-icon name="check" class="h-3.5 w-3.5" [strokeWidth]="2.6" />
            }
            {{ option }}
          </button>
        }
      </div>

      <label class="field-label mt-5" for="cancel-reason">Mensaje para el paciente (opcional)</label>
      <textarea
        id="cancel-reason"
        rows="3"
        class="field-input resize-none"
        placeholder="Escribe el motivo de la cancelación…"
        [maxLength]="maxLength"
        [value]="reason()"
        (input)="reason.set($any($event.target).value)"
      ></textarea>
      <p
        class="mt-1 text-right text-xs tabular-nums"
        [class]="reason().length >= maxLength - 50 ? 'text-tertiary' : 'text-gray-400'"
      >
        {{ reason().length }}/{{ maxLength }}
      </p>

      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" (click)="ref.close()">Volver</button>
        <button type="button" class="btn btn-danger" (click)="submit()">Cancelar cita</button>
      </div>
    </app-dialog-frame>
  `,
})
export class CancelAppointmentDialogComponent {
  protected readonly data = inject<CancelAppointmentData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<CancelAppointmentResult>>(DialogRef);

  protected readonly quickReasons = QUICK_REASONS;
  protected readonly maxLength = REASON_MAX_LENGTH;
  protected readonly reason = signal('');

  /** Picking the selected chip again clears the message. */
  protected pick(option: string): void {
    this.reason.update((current) => (current === option ? '' : option));
  }

  protected submit(): void {
    this.ref.close({ reason: this.reason().trim() || null });
  }
}
