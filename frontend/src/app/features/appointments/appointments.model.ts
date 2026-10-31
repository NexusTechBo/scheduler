import { AppointmentResponse, AppointmentStatus } from '../../core/models/api.models';

export type AppointmentStatusFilter = AppointmentStatus | 'ALL';

export interface AppointmentRow extends AppointmentResponse {
  dayLabel: string;
  timeLabel: string;
}
