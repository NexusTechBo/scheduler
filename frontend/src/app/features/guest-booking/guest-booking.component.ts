import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  afterRenderEffect,
  computed,
  inject,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { GuestSessionService } from '../../core/auth/guest-session.service';
import { datePart, formatLongDate } from '../../core/utils/calendar.util';
import { initials } from '../../core/utils/labels.util';
import { SpecialtyLabelPipe } from '../../shared/pipes/specialty-label.pipe';
import { TimeRangePipe } from '../../shared/pipes/time-range.pipe';
import { AlertComponent } from '../../shared/ui/alert/alert.component';
import { DatePickerComponent } from '../../shared/ui/forms/date-picker/date-picker.component';
import { DateStripComponent } from '../../shared/ui/forms/date-strip/date-strip.component';
import { SlotGridComponent } from '../../shared/ui/forms/slot-grid/slot-grid.component';
import { GlassBackgroundComponent } from '../../shared/ui/glass-background/glass-background.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { GuestPatientFormComponent } from './components/guest-patient-form/guest-patient-form.component';
import { GuestBookingStep } from './guest-booking.model';
import { GuestBookingService } from './guest-booking.service';

const STEP_TITLES: Record<GuestBookingStep, string> = {
  doctor: 'Elige tu doctor',
  slot: 'Elige fecha y horario',
  patient: 'Tus datos',
  summary: 'Revisa tu reserva',
  done: '¡Reserva registrada!',
};

@Component({
  selector: 'app-guest-booking',
  imports: [
    AlertComponent,
    DatePickerComponent,
    DateStripComponent,
    FormsModule,
    GlassBackgroundComponent,
    GuestPatientFormComponent,
    LoadErrorComponent,
    SkeletonComponent,
    SlotGridComponent,
    SpecialtyLabelPipe,
    TimeRangePipe,
    UiIconComponent,
  ],
  providers: [GuestBookingService],
  templateUrl: './guest-booking.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuestBookingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly session = inject(GuestSessionService);
  protected readonly booking = inject(GuestBookingService);

  protected readonly initials = initials;
  protected readonly stepTitle = computed(() => STEP_TITLES[this.booking.step()]);
  protected readonly dateLabel = computed(() => formatLongDate(this.booking.selectedDate()));
  protected readonly resultDateLabel = computed(() => {
    const result = this.booking.result();
    return result ? formatLongDate(datePart(result.startTime)) : '';
  });

  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');
  private lastView: string | null = null;

  constructor() {
    afterRenderEffect(() => {
      const view = this.session.ready() ? this.booking.step() : this.session.status();
      const heading = this.heading()?.nativeElement;
      if (this.lastView !== null && view !== this.lastView) heading?.focus();
      this.lastView = view;
    });
  }

  ngOnInit(): void {
    this.session.start(this.route.snapshot.queryParamMap.get('slug'));
  }
}
