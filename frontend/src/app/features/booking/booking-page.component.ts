import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  addDays,
  formatLongDate,
  formatMonthShort,
  formatWeekdayShort,
  parseIso,
  toIso,
} from '../../core/utils/calendar.util';
import { initials } from '../../core/utils/labels.util';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';
import { TimeRangePipe } from '../../shared/pipes/time-range.pipe';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { DatePickerComponent } from '../../shared/ui/forms/date-picker/date-picker.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { BookingService } from './services/booking.service';

const DATE_STRIP_DAYS = 14;

@Component({
  selector: 'app-booking-page',
  imports: [
    SpecialtyLabelPipe,
    TimeRangePipe,
    SkeletonComponent,
    FormsModule,
    DatePickerComponent,
    RouterLink,
    PageHeaderComponent,
    EmptyStateComponent,
    LoadErrorComponent,
    UiIconComponent,
  ],
  providers: [BookingService],
  templateUrl: './booking-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingPageComponent {
  protected readonly booking = inject(BookingService);

  protected readonly initials = initials;
  protected readonly dateLabel = computed(() => formatLongDate(this.booking.selectedDate()));
  private readonly nextDays = Array.from({ length: DATE_STRIP_DAYS }, (_, i) =>
    dayChip(addDays(new Date(), i)),
  );
  protected readonly dateStrip = computed(() => {
    const selected = this.booking.selectedDate();
    return this.nextDays.some((day) => day.iso === selected)
      ? this.nextDays
      : [...this.nextDays, dayChip(parseIso(selected))];
  });
}

function dayChip(date: Date) {
  return {
    iso: toIso(date),
    weekday: formatWeekdayShort(date),
    day: date.getDate(),
    month: formatMonthShort(date),
  };
}
