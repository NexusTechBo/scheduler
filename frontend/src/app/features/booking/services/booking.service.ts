import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, map } from 'rxjs';
import { AgendaApi } from '../../../core/api/agenda.api';
import { AppointmentsApi } from '../../../core/api/appointments.api';
import { CatalogsApi } from '../../../core/api/catalogs.api';
import { PatientsApi } from '../../../core/api/patients.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage, resourceError } from '../../../core/http/api-error';
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

  private readonly _patientSearch = signal('');
  private readonly _selectedPatient = signal<Patient | null>(null);
  readonly patientSearch = this._patientSearch.asReadonly();
  readonly selectedPatient = this._selectedPatient.asReadonly();
  private readonly debouncedPatientSearch = toSignal(
    toObservable(this._patientSearch).pipe(
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

  private readonly _specialtyId = signal<number | null>(null);
  readonly specialtyId = this._specialtyId.asReadonly();
  private readonly _selectedDoctor = linkedSignal<DoctorOption | null>(() =>
    this.isDoctor
      ? { id: this.auth.userId()!, name: this.auth.displayName(), specialtyName: null }
      : null,
  );
  readonly selectedDoctor = this._selectedDoctor.asReadonly();
  private readonly selectedDoctorId = computed(() => this.selectedDoctor()?.id);
  private readonly specialtiesResource = rxResource({
    request: () => (this.isDoctor ? undefined : true),
    loader: () => this.catalogsApi.specialties(),
  });
  private readonly doctorsResource = rxResource({
    request: () => (this.isDoctor ? undefined : { specialtyId: this._specialtyId() ?? undefined }),
    loader: ({ request }) =>
      this.staffApi
        .doctors({ specialtyId: request!.specialtyId, isActive: true, size: 100 })
        .pipe(map((page) => page.content)),
  });
  readonly specialties = computed(() => this.specialtiesResource.value() ?? []);
  readonly doctors = computed(() => this.doctorsResource.value() ?? []);
  readonly doctorsLoading = this.doctorsResource.isLoading;

  private readonly dateParam = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map((params) => params.get('date'))),
  );
  private readonly _selectedDate = linkedSignal(() => {
    const requested = this.dateParam();
    return isIsoDate(requested) && requested >= this.today ? requested : this.today;
  });
  readonly selectedDate = this._selectedDate.asReadonly();
  private readonly _selectedSlot = linkedSignal<unknown, TimeSlot | null>({
    source: () => [this.selectedDoctorId(), this.selectedDate()],
    computation: () => null,
  });
  readonly selectedSlot = this._selectedSlot.asReadonly();
  private readonly slotsResource = rxResource({
    request: () => {
      const doctorId = this.selectedDoctorId();
      return doctorId ? { doctorId, date: this.selectedDate() } : undefined;
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
  readonly slots = computed(() => this.slotsResource.value() ?? []);
  readonly slotsLoading = this.slotsResource.isLoading;
  readonly slotsErrorMessage = resourceError(
    this.slotsResource,
    'No se pudieron cargar los horarios',
  );

  private readonly _submitting = signal(false);
  readonly submitting = this._submitting.asReadonly();
  readonly canSubmit = computed(
    () =>
      !!this.selectedDoctor() &&
      !!this.selectedSlot() &&
      !!this.selectedPatient() &&
      !this.submitting(),
  );

  setPatientSearch(search: string): void {
    this._patientSearch.set(search);
  }

  selectPatient(patient: Patient | null): void {
    this._selectedPatient.set(patient);
  }

  selectSpecialty(id: number | null): void {
    this._specialtyId.set(id);
    if (id === null) return;
    const name = this.specialties().find((s) => s.id === id)?.name;
    if (this.selectedDoctor()?.specialtyName !== name) this._selectedDoctor.set(null);
  }

  selectDoctor(doctor: Staff): void {
    this._selectedDoctor.set({
      id: doctor.id,
      name: doctor.name,
      specialtyName: doctor.specialtyName,
    });
  }

  selectDate(iso: string): void {
    if (iso && iso >= this.today) this._selectedDate.set(iso);
  }

  selectSlot(slot: TimeSlot): void {
    this._selectedSlot.set(slot);
  }

  reloadSlots(): void {
    this.slotsResource.reload();
  }

  book(): void {
    const doctor = this.selectedDoctor();
    const slot = this.selectedSlot();
    const patient = this.selectedPatient();
    if (!this.canSubmit() || !doctor || !slot || !patient) return;

    const date = this.selectedDate();
    this._submitting.set(true);
    this.appointmentsApi
      .book({
        doctorId: doctor.id,
        patientId: patient.id,
        startTime: toLocalDateTime(date, slot.startTime),
      })
      .pipe(finalize(() => this._submitting.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Cita agendada: queda pendiente de confirmación');
          this.router.navigate(['/calendar'], { queryParams: { date } }).finally();
        },
        error: (error) => {
          this.notifications.error(apiErrorMessage(error, 'No se pudo agendar la cita'));
          this._selectedSlot.set(null);
          this.slotsResource.reload();
        },
      });
  }
}
