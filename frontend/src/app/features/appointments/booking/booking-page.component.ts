import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DateRangePickerComponent } from '../../../shared/ui/date-range-picker/date-range-picker.component';
import { Router, RouterLink } from '@angular/router';
import { finalize, map, of } from 'rxjs';
import { AgendaApi } from '../../../core/api/agenda.api';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { CatalogsApi } from '../../../core/api/catalogs.api';
import { PatientsApi } from '../../../core/api/patients.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Patient, Staff, TimeSlot } from '../../../core/models/api.models';
import {
  addDays,
  formatLongDate,
  formatTime,
  isIsoDate,
  toIso,
  toLocalDateTime,
} from '../../../core/utils/calendar.util';
import { initials } from '../../../core/utils/labels.util';
import { NotificationService } from '../../../shared/services/notification.service';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../../shared/ui/ui-icon/ui-icon.component';

const DATE_STRIP_DAYS = 14;

interface DoctorOption {
  id: number;
  name: string;
  specialtyName: string | null;
}

@Component({
  selector: 'app-booking-page',
  imports: [
    FormsModule,
    DateRangePickerComponent,
    RouterLink,
    PageHeaderComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  templateUrl: './booking-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class BookingPageComponent {
  private readonly auth = inject(AuthService);
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly agendaApi = inject(AgendaApi);
  private readonly staffApi = inject(StaffApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly catalogsApi = inject(CatalogsApi);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  readonly date = input<string>();

  protected readonly isPatient = this.auth.hasRole('PATIENT');
  protected readonly isDoctor = this.auth.hasRole('DOCTOR');
  protected readonly today = toIso(new Date());

  protected readonly patientSearch = signal('');
  protected readonly selectedPatient = signal<Patient | null>(null);
  private readonly patientsResource = rxResource({
    loader: () =>
      this.isPatient
        ? of([] as Patient[])
        : this.patientsApi
            .list({ size: 1000, sort: 'id' })
            .pipe(map((page) => page.content.filter((p) => p.active))),
  });
  protected readonly patientsLoading = this.patientsResource.isLoading;
  protected readonly filteredPatients = computed(() => {
    const term = this.patientSearch().trim().toLowerCase();
    const patients = this.patientsResource.value() ?? [];
    return (
      term ? patients.filter((p) => `${p.name} ${p.email}`.toLowerCase().includes(term)) : patients
    ).slice(0, 50);
  });

  protected readonly specialtyId = signal<number | null>(null);
  protected readonly selectedDoctor = signal<DoctorOption | null>(
    this.isDoctor
      ? { id: this.auth.userId()!, name: this.auth.session()!.username, specialtyName: null }
      : null,
  );
  protected readonly specialtiesResource = rxResource({
    loader: () => (this.isDoctor ? of([]) : this.catalogsApi.specialties()),
  });
  protected readonly doctorsResource = rxResource({
    request: () => ({ specialtyId: this.specialtyId() }),
    loader: ({ request }) =>
      this.isDoctor
        ? of([] as Staff[])
        : this.staffApi
            .doctors({ specialtyId: request.specialtyId ?? undefined, isActive: true, size: 100 })
            .pipe(map((page) => page.content)),
  });

  protected readonly selectedDate = signal(this.today);
  protected readonly dateStrip = Array.from({ length: DATE_STRIP_DAYS }, (_, i) => {
    const date = addDays(new Date(), i);
    return {
      iso: toIso(date),
      weekday: date.toLocaleDateString('es-ES', { weekday: 'short' }).replace('.', ''),
      day: date.getDate(),
      month: date.toLocaleDateString('es-ES', { month: 'short' }).replace('.', ''),
    };
  });
  protected readonly selectedSlot = signal<TimeSlot | null>(null);

  protected readonly slotsResource = rxResource({
    request: () => {
      const doctor = this.selectedDoctor();
      return doctor ? { doctorId: doctor.id, date: this.selectedDate() } : undefined;
    },
    loader: ({ request }) =>
      this.agendaApi.availableSlots(request!.doctorId, request!.date).pipe(
        map((res) =>
          res.availableSlots.map((s) => ({
            startTime: formatTime(s.startTime),
            endTime: formatTime(s.endTime),
          })),
        ),
      ),
  });

  protected readonly submitting = signal(false);

  protected readonly dateLabel = computed(() => formatLongDate(this.selectedDate()));
  protected readonly canSubmit = computed(
    () =>
      !!this.selectedDoctor() &&
      !!this.selectedSlot() &&
      (this.isPatient || !!this.selectedPatient()) &&
      !this.submitting(),
  );

  protected readonly initials = initials;

  constructor() {
    effect(() => {
      const requested = this.date();
      if (isIsoDate(requested) && requested >= this.today) this.selectedDate.set(requested);
    });
  }

  protected selectSpecialty(id: number | null): void {
    this.specialtyId.set(id);
    if (this.isDoctor || id === null) return;
    const name = this.specialtiesResource.value()?.find((s) => s.id === id)?.name;
    if (this.selectedDoctor()?.specialtyName !== name) this.selectDoctor(null);
  }

  protected selectDoctor(doctor: Staff | null): void {
    this.selectedDoctor.set(
      doctor ? { id: doctor.id, name: doctor.name, specialtyName: doctor.specialtyName } : null,
    );
    this.selectedSlot.set(null);
  }

  protected selectDate(iso: string): void {
    if (!iso || iso < this.today) return;
    this.selectedDate.set(iso);
    this.selectedSlot.set(null);
  }

  protected submit(): void {
    if (this.submitting()) return;
    const doctor = this.selectedDoctor();
    const slot = this.selectedSlot();
    const patientId = this.isPatient ? this.auth.userId() : this.selectedPatient()?.id;
    if (!doctor || !slot || !patientId) return;

    this.submitting.set(true);
    this.appointmentsApi
      .book({
        doctorId: doctor.id,
        patientId,
        startTime: toLocalDateTime(this.selectedDate(), slot.startTime),
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success(
            this.isPatient
              ? 'Solicitud enviada: la clínica confirmará tu cita'
              : 'Cita agendada: queda pendiente de confirmación',
          );
          if (this.isPatient) this.router.navigate(['/appointments']).finally();
          else
            this.router
              .navigate(['/calendar'], { queryParams: { date: this.selectedDate() } })
              .finally();
        },
        error: (error) => {
          this.notifications.error(apiErrorMessage(error, 'No se pudo agendar la cita'));
          this.selectedSlot.set(null);
          this.slotsResource.reload();
        },
      });
  }
}
