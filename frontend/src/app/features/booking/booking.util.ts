import { PatientActiveAppointmentConflict } from '../../core/models/api.models';
import { datePart, formatLongDate, formatTime } from '../../core/utils/calendar.util';
import { STATUS_LABEL } from '../../core/utils/labels.util';
import { ConfirmOptions } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { BookingAttempt } from './booking.model';

export function rescheduleConfirmOptions(
  conflict: PatientActiveAppointmentConflict,
  attempt: BookingAttempt,
): ConfirmOptions {
  const status = STATUS_LABEL[conflict.status].toLowerCase();
  const currentDate = formatLongDate(datePart(conflict.startTime)).toLowerCase();
  const newDate = formatLongDate(attempt.date).toLowerCase();
  return {
    title: 'Paciente con cita activa',
    message:
      `${attempt.patient.name} ya tiene una cita ${status} con ${attempt.doctor.name} ` +
      `el ${currentDate} a las ${formatTime(conflict.startTime)}. ` +
      `¿Deseas reprogramarla al ${newDate} a las ${formatTime(attempt.startDateTime)}?`,
    confirmLabel: 'Reprogramar',
  };
}
