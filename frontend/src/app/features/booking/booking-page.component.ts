import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { addDays, formatLongDate, parseIso, toIso } from '../../core/utils/calendar.util';
import { initials } from '../../core/utils/labels.util';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { DatePickerComponent } from '../../shared/ui/forms/date-picker/date-picker.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { BookingService } from './services/booking.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';

const DATE_STRIP_DAYS = 14;

@Component({
  selector: 'app-booking-page',
  imports: [
    SpecialtyLabelPipe,
    SkeletonComponent,
    FormsModule,
    DatePickerComponent,
    RouterLink,
    PageHeaderComponent,
    EmptyStateComponent,
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
    weekday: date.toLocaleDateString('es-ES', { weekday: 'short' }).replace('.', ''),
    day: date.getDate(),
    month: date.toLocaleDateString('es-ES', { month: 'short' }).replace('.', ''),
  };
}
