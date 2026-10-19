import { AppointmentStatus, AppointmentSummaryItem } from './api.models';

export interface BoardColumn {
  id: AppointmentStatus;
  title: string;
  accentClass: string;
  appointments: AppointmentSummaryItem[];
}
