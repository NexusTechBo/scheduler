import { Dialog } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Patient } from '../../core/models/api.models';
import { formatLongDate } from '../../core/utils/calendar.util';
import { DoctorPickerComponent } from '../../shared/components/doctor-picker/doctor-picker.component';
import {
  PatientFormData,
  PatientFormDialogComponent,
} from '../../shared/components/patient-form-dialog/patient-form-dialog.component';
import { SlotGridComponent } from '../../shared/components/slot-grid/slot-grid.component';
import { SpecialtyFilterComponent } from '../../shared/components/specialty-filter/specialty-filter.component';
import { InitialsPipe } from '../../shared/pipes/initials.pipe';
import { TimeRangePipe } from '../../shared/pipes/time-range.pipe';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { DatePickerComponent } from '../../shared/ui/forms/date-picker/date-picker.component';
import { DateStripComponent } from '../../shared/ui/forms/date-strip/date-strip.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { BookingService } from './booking.service';

@Component({
  selector: 'app-booking-page',
  imports: [
    InitialsPipe,
    TimeRangePipe,
    SkeletonComponent,
    FormsModule,
    DatePickerComponent,
    DateStripComponent,
    SlotGridComponent,
    SpecialtyFilterComponent,
    DoctorPickerComponent,
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
  private readonly dialog = inject(Dialog);
  protected readonly booking = inject(BookingService);

  protected readonly dateLabel = computed(() => formatLongDate(this.booking.selectedDate()));

  protected registerPatient(): void {
    this.dialog
      .open<Patient, PatientFormData>(PatientFormDialogComponent, {
        ...labelledByFrame(),
        data: {},
      })
      .closed.subscribe((patient) => {
        if (!patient) return;
        this.booking.selectPatient(patient);
        this.booking.reloadPatients();
      });
  }
}
