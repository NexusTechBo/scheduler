import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { DialogFrameComponent } from '../../../ui/dialog-frame/dialog-frame.component';
import { UiIconComponent } from '../../../ui/ui-icon/ui-icon.component';

export interface CancelAppointmentData {
  patientName: string;
  when: string;
}

export interface CancelAppointmentResult {
  reason: string;
}

const REASON_MAX_LENGTH = 500;

const QUICK_REASONS = [
  'El doctor no estará disponible',
  'Solicitado por el paciente',
  'Emergencia médica',
  'Se reprogramará la cita',
  'Cambio de horario de la clínica',
];

@Component({
  selector: 'app-cancel-appointment-dialog',
  imports: [DialogFrameComponent, UiIconComponent],
  templateUrl: './cancel-appointment-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CancelAppointmentDialogComponent {
  protected readonly data = inject<CancelAppointmentData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<CancelAppointmentResult>>(DialogRef);

  protected readonly quickReasons = QUICK_REASONS;
  protected readonly maxLength = REASON_MAX_LENGTH;
  protected readonly reason = signal('');

  protected pick(option: string): void {
    this.reason.update((current) => (current === option ? '' : option));
  }

  protected submit(): void {
    this.ref.close({ reason: this.reason().trim() });
  }
}
