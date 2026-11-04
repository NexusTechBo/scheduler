import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, map } from 'rxjs';
import { AgendaApi } from '../../core/api/agenda.api';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { PatientsApi } from '../../core/api/patients.api';
import { AuthService } from '../../core/auth/auth.service';
import { DoctorsStore } from '../../core/catalogs/doctors.store';
import { SpecialtiesStore } from '../../core/catalogs/specialties.store';
import { apiErrorMessage, resourceError } from '../../core/http/api-error';
import { Patient, Staff, TimeSlot } from '../../core/models/api.models';
import { formatTime, isIsoDate, toIso, toLocalDateTime } from '../../core/utils/calendar.util';
import { LOAD_ERROR } from '../../core/utils/messages.util';
import { NotificationService } from '../../shared/services/notification.service';
import { DoctorOption } from './booking.model';

const PATIENT_RESULTS = 20;

@Injectable()
export class BookingService {
  private readonly auth = inject(AuthService);
  private readonly appointmentsApi = inject(AppointmentsApi);
  private readonly agendaApi = inject(AgendaApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly specialtiesStore = inject(SpecialtiesStore);
  private readonly doctorsStore = inject(DoctorsStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  readonly ownDataOnly = this.auth.restrictedToOwnData();
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
    params: () => this.debouncedPatientSearch(),
    stream: ({ params }) =>
      this.patientsApi
        .list({ search: params, isActive: true, size: PATIENT_RESULTS, sort: 'id' })
        .pipe(map((page) => page.content)),
  });
  readonly patientsLoading = this.patientsResource.isLoading;
  readonly filteredPatients = computed(() =>
    this.patientsResource.hasValue() ? this.patientsResource.value() : [],
  );
  readonly patientsErrorMessage = resourceError(this.patientsResource, LOAD_ERROR.patients);

  private readonly _specialtyId = signal<number | null>(null);
  readonly specialtyId = this._specialtyId.asReadonly();
  private readonly _selectedDoctor = linkedSignal<DoctorOption | null>(() =>
    this.ownDataOnly
      ? {
          id: this.auth.userId()!,
          name: this.auth.displayName(),
          specialtyId: null,
          specialtyName: null,
        }
      : null,
  );
  readonly selectedDoctor = this._selectedDoctor.asReadonly();
  private readonly selectedDoctorId = computed(() => this.selectedDoctor()?.id);
  readonly specialties = this.specialtiesStore.specialties;
  readonly doctors = computed(() => {
    const all = this.doctorsStore.doctors();
    const id = this._specialtyId();
    return id === null ? all : all.filter((doctor) => doctor.specialtyId === id);
  });
  readonly doctorsLoading = this.doctorsStore.isLoading;
  readonly specialtiesErrorMessage = this.specialtiesStore.errorMessage;
  readonly doctorsErrorMessage = this.doctorsStore.errorMessage;

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
    params: () => {
      const doctorId = this.selectedDoctorId();
      return doctorId ? { doctorId, date: this.selectedDate() } : undefined;
    },
    stream: ({ params }) =>
      this.agendaApi.availableSlots(params.doctorId, params.date).pipe(
        map((response) =>
          response.availableSlots.map((slot) => ({
            startTime: formatTime(slot.startTime),
            endTime: formatTime(slot.endTime),
          })),
        ),
      ),
  });
  readonly slots = computed(() =>
    this.slotsResource.hasValue() ? this.slotsResource.value() : [],
  );
  readonly slotsLoading = this.slotsResource.isLoading;
  readonly slotsErrorMessage = resourceError(this.slotsResource, LOAD_ERROR.slots);

  private readonly _submitting = signal(false);
  readonly submitting = this._submitting.asReadonly();
  readonly canSubmit = computed(
    () =>
      !!this.selectedDoctor() &&
      !!this.selectedSlot() &&
      !!this.selectedPatient() &&
      !this.submitting(),
  );

  constructor() {
    if (this.ownDataOnly) return;
    this.specialtiesStore.load();
    this.doctorsStore.load();
  }

  setPatientSearch(search: string): void {
    this._patientSearch.set(search);
  }

  selectPatient(patient: Patient | null): void {
    this._selectedPatient.set(patient);
  }

  selectSpecialty(id: number | null): void {
    if (this.ownDataOnly) return;
    this._specialtyId.set(id);
    if (id !== null && this.selectedDoctor()?.specialtyId !== id) this._selectedDoctor.set(null);
  }

  selectDoctor(doctor: Staff): void {
    if (this.ownDataOnly) return;
    this._selectedDoctor.set({
      id: doctor.id,
      name: doctor.name,
      specialtyId: doctor.specialtyId,
      specialtyName: doctor.specialtyName,
    });
  }

  selectDate(iso: string): void {
    if (iso && iso >= this.today) this._selectedDate.set(iso);
  }

  selectSlot(slot: TimeSlot): void {
    this._selectedSlot.set(slot);
  }

  reloadPatients(): void {
    this.patientsResource.reload();
  }

  reloadSpecialties(): void {
    this.specialtiesStore.reload();
  }

  reloadDoctors(): void {
    this.doctorsStore.reload();
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
