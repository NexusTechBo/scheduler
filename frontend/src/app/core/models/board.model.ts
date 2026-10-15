import { AppointmentStatus, AppointmentSummaryItem } from './api.models';

/** One Kanban column. The board only ever has the 3 backend statuses — no custom workflows. */
export interface BoardColumn {
  id: AppointmentStatus;
  title: string;
  accentClass: string;
  appointments: AppointmentSummaryItem[];
}
