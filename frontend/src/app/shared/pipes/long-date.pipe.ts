import { Pipe, PipeTransform } from '@angular/core';
import { formatLongDate } from '../../core/utils/calendar.util';

@Pipe({ name: 'longDate' })
export class LongDatePipe implements PipeTransform {
  transform(iso: string): string {
    return formatLongDate(iso);
  }
}
