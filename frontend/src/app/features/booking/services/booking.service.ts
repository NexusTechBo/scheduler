import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, map, of } from 'rxjs';
import { AgendaApi } from '../../../core/api/agenda.api';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { CatalogsApi } from '../../../core/api/catalogs.api';
import { PatientsApi } from '../../../core/api/patients.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Patient, Staff, TimeSlot } from '../../../core/models/api.models';
import { formatTime, isIsoDate, toIso, toLocalDateTime } from '../../../core/utils/calendar.util';
import { NotificationService } from '../../../shared/services/notification.service';

const PATIENT_RESULTS = 20;

type DoctorOption = Pick<Staff, 'id' | 'name' | 'specialtyName'>;

@Injectable()
export class BookingService {
  private readonly auth = inject(AuthService);
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly agendaApi = inject(AgendaApi);
  private readonly staffApi = inject(StaffApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly catalogsApi = inject(CatalogsApi);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  readonly isDoctor = this.auth.hasRole('DOCTOR');
  readonly today = toIso(new Date());

  readonly patientSearch = signal('');
  readonly selectedPatient = signal<Patient | null>(null);
  private readonly debouncedPatientSearch = toSignal(
    toObservable(this.patientSearch).pipe(
      debounceTime(500),
      map((search) => search.trim()),
      distinctUntilChanged(),
    ),
    { initialValue: '' },
  );
  private readonly patientsResource = rxResource({
    request: () => this.debouncedPatientSearch(),
    loader: ({ request }) =>
      this.patientsApi
        .list({ search: request, isActive: true, size: PATIENT_RESULTS, sort: 'id' })
        .pipe(map((page) => page.content)),
  });
  readonly patientsLoading = this.patientsResource.isLoading;
  readonly filteredPatients = computed(() => this.patientsResource.value() ?? []);

  readonly specialtyId = signal<number | null>(null);
  readonly selectedDoctor = signal<DoctorOption | null>(
    this.isDoctor
      ? { id: this.auth.userId()!, name: this.auth.session()!.username, specialtyName: null }
      : null,
  );
  readonly specialtiesResource = rxResource({
    loader: () => (this.isDoctor ? of([]) : this.catalogsApi.specialties()),
  });
  readonly doctorsResource = rxResource({
    request: () => ({ specialtyId: this.specialtyId() ?? undefined }),
    loader: ({ request }) =>
      this.isDoctor
        ? of([] as Staff[])
        : this.staffApi
            .doctors({ specialtyId: request.specialtyId, isActive: true, size: 100 })
            .pipe(map((page) => page.content)),
  });

  private readonly dateParam = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map((params) => params.get('date'))),
  );
  readonly selectedDate = linkedSignal(() => {
    const requested = this.dateParam();
    return isIsoDate(requested) && requested >= this.today ? requested : this.today;
  });
  readonly selectedSlot = linkedSignal<unknown, TimeSlot | null>({
    source: () => [this.selectedDoctor(), this.selectedDate()],
    computation: () => null,
  });
  readonly slotsResource = rxResource({
    request: () => {
      const doctor = this.selectedDoctor();
      return doctor ? { doctorId: doctor.id, date: this.selectedDate() } : undefined;
    },
    loader: ({ request }) =>
      this.agendaApi.availableSlots(request!.doctorId, request!.date).pipe(
        map((response) =>
          response.availableSlots.map((slot) => ({
            startTime: formatTime(slot.startTime),
            endTime: formatTime(slot.endTime),
          })),
        ),
      ),
  });

  readonly submitting = signal(false);
  readonly canSubmit = computed(
    () =>
      !!this.selectedDoctor() &&
      !!this.selectedSlot() &&
      !!this.selectedPatient() &&
      !this.submitting(),
  );

  selectSpecialty(id: number | null): void {
    this.specialtyId.set(id);
    if (id === null) return;
    const name = this.specialtiesResource.value()?.find((s) => s.id === id)?.name;
    if (this.selectedDoctor()?.specialtyName !== name) this.selectedDoctor.set(null);
  }

  selectDoctor(doctor: Staff): void {
    this.selectedDoctor.set({
      id: doctor.id,
      name: doctor.name,
      specialtyName: doctor.specialtyName,
    });
  }

  selectDate(iso: string): void {
    if (iso && iso >= this.today) this.selectedDate.set(iso);
  }

  book(): void {
    const doctor = this.selectedDoctor();
    const slot = this.selectedSlot();
    const patient = this.selectedPatient();
    if (!this.canSubmit() || !doctor || !slot || !patient) return;

    const date = this.selectedDate();
    this.submitting.set(true);
    this.appointmentsApi
      .book({
        doctorId: doctor.id,
        patientId: patient.id,
        startTime: toLocalDateTime(date, slot.startTime),
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Cita agendada: queda pendiente de confirmación');
          this.router.navigate(['/calendar'], { queryParams: { date } }).finally();
        },
        error: (error) => {
          this.notifications.error(apiErrorMessage(error, 'No se pudo agendar la cita'));
          this.selectedSlot.set(null);
          this.slotsResource.reload();
        },
      });
  }
}
