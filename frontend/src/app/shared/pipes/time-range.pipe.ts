import { Pipe, PipeTransform } from '@angular/core';
import { formatTimeRange } from '../../core/utils/calendar.util';

@Pipe({ name: 'timeRange' })
export class TimeRangePipe implements PipeTransform {
  transform({ startTime, endTime }: { startTime: string; endTime: string }): string {
    return formatTimeRange(startTime, endTime);
  }
}
