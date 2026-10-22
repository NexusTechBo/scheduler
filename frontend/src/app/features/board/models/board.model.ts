import { AppointmentStatus, AppointmentSummaryItem } from '../../../core/models/api.models';

export interface BoardColumn {
  id: AppointmentStatus;
  title: string;
  accentClass: string;
  appointments: AppointmentSummaryItem[];
}
