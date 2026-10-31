import { AvailabilityRequest, DayOfWeek } from '../../core/models/api.models';

export type AvailabilityBlock = Omit<AvailabilityRequest, 'dayOfWeek'>;

export interface AddAvailabilityResult {
  saved: DayOfWeek[];
  failed: { day: DayOfWeek; message: string }[];
}
