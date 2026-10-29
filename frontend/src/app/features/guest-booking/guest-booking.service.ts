import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, finalize, ignoreElements, map, tap } from 'rxjs';
import { GuestApi } from '../../core/api/guest.api';
import { GuestSessionService } from '../../core/auth/guest-session.service';
import { apiErrorMessage, resourceError } from '../../core/http/api-error';
import {
  GuestAppointmentResponse,
  GuestDoctor,
  GuestBookingPatient,
  TimeSlot,
} from '../../core/models/api.models';
import { formatTime, toIso, toLocalDateTime } from '../../core/utils/calendar.util';

const GUEST_BOOKING_STEPS = ['doctor', 'slot', 'patient', 'summary', 'done'] as const;

export type GuestBookingStep = (typeof GUEST_BOOKING_STEPS)[number];

@Injectable()
export class GuestBookingService {
  private readonly guestApi = inject(GuestApi);
  private readonly guestSession = inject(GuestSessionService);

  readonly today = toIso(new Date());

  private readonly active = computed(() => (this.guestSession.ready() ? true : undefined));

  private readonly clinicResource = rxResource({
    request: () => this.active(),
    loader: () => this.guestApi.clinic(),
  });
  readonly clinicName = computed(() => this.clinicResource.value()?.name ?? null);

  private readonly _step = signal<GuestBookingStep>('doctor');
  readonly step = this._step.asReadonly();
  readonly stepNumber = computed(() => GUEST_BOOKING_STEPS.indexOf(this._step()) + 1);
  readonly totalSteps = GUEST_BOOKING_STEPS.length - 1;

  private readonly _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  private readonly _specialtyId = signal<number | null>(null);
  readonly specialtyId = this._specialtyId.asReadonly();
  private readonly specialtiesResource = rxResource({
    request: () => this.active(),
    loader: () =>
      this.guestApi
        .specialties()
        .pipe(map((list) => list.sort((a, b) => a.name.localeCompare(b.name)))),
  });
  readonly specialties = computed(() => this.specialtiesResource.value() ?? []);
  readonly specialtiesErrorMessage = resourceError(
    this.specialtiesResource,
    'No se pudieron cargar las especialidades',
  );

  private readonly doctorsResource = rxResource({
    request: () => this.active() && { specialtyId: this._specialtyId() ?? undefined },
    loader: ({ request }) => this.guestApi.doctors(request!.specialtyId),
  });
  readonly doctors = computed(() => this.doctorsResource.value() ?? []);
  readonly doctorsLoading = this.doctorsResource.isLoading;
  readonly doctorsErrorMessage = resourceError(
    this.doctorsResource,
    'No se pudieron cargar los doctores',
  );

  private readonly _selectedDoctor = signal<GuestDoctor | null>(null);
  readonly selectedDoctor = this._selectedDoctor.asReadonly();
  private readonly _selectedDate = signal(this.today);
  readonly selectedDate = this._selectedDate.asReadonly();
  private readonly _selectedSlot = linkedSignal<unknown, TimeSlot | null>({
    source: () => [this._selectedDoctor()?.id, this._selectedDate()],
    computation: () => null,
  });
  readonly selectedSlot = this._selectedSlot.asReadonly();

  private readonly slotsResource = rxResource({
    request: () => {
      const doctorId = this._selectedDoctor()?.id;
      return this.active() && doctorId ? { doctorId, date: this._selectedDate() } : undefined;
    },
    loader: ({ request }) => this.loadSlots(request!.doctorId, request!.date),
  });
  readonly slots = computed(() => this.slotsResource.value() ?? []);
  readonly slotsLoading = this.slotsResource.isLoading;
  readonly slotsErrorMessage = resourceError(
    this.slotsResource,
    'No se pudieron cargar los horarios',
  );

  private readonly _patient = signal<GuestBookingPatient | null>(null);
  readonly patient = this._patient.asReadonly();

  private readonly _submitting = signal(false);
  readonly submitting = this._submitting.asReadonly();
  private readonly _result = signal<GuestAppointmentResponse | null>(null);
  readonly result = this._result.asReadonly();

  reloadSpecialties(): void {
    this.specialtiesResource.reload();
  }

  reloadDoctors(): void {
    this.doctorsResource.reload();
  }

  reloadSlots(): void {
    this.slotsResource.reload();
  }

  selectSpecialty(id: number | null): void {
    this._specialtyId.set(id);
    const doctor = this._selectedDoctor();
    if (id !== null && doctor && doctor.specialtyId !== id) this._selectedDoctor.set(null);
  }

  selectDoctor(doctor: GuestDoctor): void {
    this._selectedDoctor.set(doctor);
    this.goTo('slot');
  }

  selectDate(iso: string): void {
    if (iso && iso >= this.today) this._selectedDate.set(iso);
  }

  selectSlot(slot: TimeSlot): void {
    this._selectedSlot.set(slot);
    this.goTo('patient');
  }

  setPatient(patient: GuestBookingPatient): void {
    this._patient.set(patient);
    this.goTo('summary');
  }

  back(): void {
    const index = GUEST_BOOKING_STEPS.indexOf(this._step());
    if (index > 0 && this._step() !== 'done') this.goTo(GUEST_BOOKING_STEPS[index - 1]);
  }

  bookAnother(): void {
    this._result.set(null);
    this._selectedDoctor.set(null);
    this._selectedDate.set(this.today);
    this.goTo('doctor');
  }

  book(): void {
    const doctor = this._selectedDoctor();
    const slot = this._selectedSlot();
    const patient = this._patient();
    if (this._submitting() || !doctor || !slot || !patient) return;

    const date = this._selectedDate();
    this._submitting.set(true);
    this._error.set(null);
    this.guestApi
      .book({ doctorId: doctor.id, startTime: toLocalDateTime(date, slot.startTime), patient })
      .pipe(
        tap((result) => {
          this._result.set(result);
          this.goTo('done');
        }),
        catchError((error: unknown) => this.handleBookingError(error, doctor.id, date, slot)),
        finalize(() => this._submitting.set(false)),
      )
      .subscribe();
  }

  private handleBookingError(
    error: unknown,
    doctorId: number,
    date: string,
    slot: TimeSlot,
  ): Observable<never> {
    const message = apiErrorMessage(error, 'No se pudo registrar la reserva');
    if (!(error instanceof HttpErrorResponse) || error.status !== HttpStatusCode.Conflict) {
      this._error.set(message);
      return EMPTY;
    }
    const stillSelected = () =>
      this._selectedDoctor()?.id === doctorId && this._selectedDate() === date;
    return this.loadSlots(doctorId, date).pipe(
      tap((slots) => {
        if (!stillSelected()) return;
        this.slotsResource.set(slots);
        const stillFree = slots.some((s) => s.startTime === slot.startTime);
        if (!stillFree) this._selectedSlot.set(null);
        this.goTo(stillFree ? 'patient' : 'slot', message);
      }),
      catchError(() => {
        if (stillSelected()) {
          this._selectedSlot.set(null);
          this.slotsResource.reload();
          this.goTo('slot', message);
        }
        return EMPTY;
      }),
      ignoreElements(),
    );
  }

  private loadSlots(doctorId: number, date: string): Observable<TimeSlot[]> {
    return this.guestApi.availableSlots(doctorId, date).pipe(
      map((response) =>
        response.availableSlots.map((slot) => ({
          startTime: formatTime(slot.startTime),
          endTime: formatTime(slot.endTime),
        })),
      ),
    );
  }

  private goTo(step: GuestBookingStep, error: string | null = null): void {
    this._error.set(error);
    this._step.set(step);
  }
}
