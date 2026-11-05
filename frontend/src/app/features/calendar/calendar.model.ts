import { AppointmentSummaryItem } from '../../core/models/api.models';
import { CalendarDay } from '../../core/utils/calendar.util';

export type CalendarScope = 'all' | 'mine';

export interface CalendarCell {
  day: CalendarDay;
  appointments: AppointmentSummaryItem[] | null;
}
