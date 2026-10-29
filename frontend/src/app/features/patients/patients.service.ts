import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { PatientsApi } from '../../core/api/patients.api';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage, resourceError } from '../../core/http/api-error';
import { Patient, PatientRequest } from '../../core/models/api.models';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';

const PATIENTS_PAGE_SIZE = 12;

export type PatientScope = 'mine' | 'all';

@Injectable()
export class PatientsService {
  private readonly patientsApi = inject(PatientsApi);
  private readonly staffApi = inject(StaffApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly userId = inject(AuthService).userId()!;

  readonly isDoctor = inject(AuthService).hasRole('DOCTOR');

  private readonly _scope = signal<PatientScope>(this.isDoctor ? 'mine' : 'all');
  private readonly _search = signal('');
  private readonly _showInactive = signal(false);
  private readonly page = signal(0);
  readonly scope = this._scope.asReadonly();
  readonly search = this._search.asReadonly();
  readonly showInactive = this._showInactive.asReadonly();
  private readonly debouncedSearch = toSignal(
    toObservable(this._search).pipe(
      debounceTime(500),
      map((search) => search.trim()),
      distinctUntilChanged(),
      tap(() => this.page.set(0)),
    ),
    { initialValue: '' },
  );

  private readonly allResource = rxResource({
    request: () =>
      this.scope() === 'all'
        ? {
            search: this.debouncedSearch(),
            isActive: this.showInactive() ? undefined : true,
            page: this.page(),
          }
        : undefined,
    loader: ({ request }) =>
      this.patientsApi.list({ ...request, size: PATIENTS_PAGE_SIZE, sort: 'id' }),
  });
  private readonly mineResource = rxResource({
    request: () => (this.isDoctor ? this.userId : undefined),
    loader: ({ request }) => this.staffApi.patientsOf(request),
  });

  private readonly mineFiltered = computed(() => {
    const showInactive = this.showInactive();
    const term = this.debouncedSearch().toLowerCase();
    return (this.mineResource.value() ?? [])
      .filter((patient) => showInactive || patient.active)
      .filter(
        (patient) =>
          !term ||
          `${patient.name} ${patient.ci} ${patient.email} ${patient.phoneNumber ?? ''}`
            .toLowerCase()
            .includes(term),
      );
  });
  readonly patients = computed(() => {
    if (this.scope() === 'all') return this.allResource.value()?.content ?? [];
    const start = this.page() * PATIENTS_PAGE_SIZE;
    return this.mineFiltered().slice(start, start + PATIENTS_PAGE_SIZE);
  });
  readonly pagination = computed(() => {
    if (this.scope() === 'all') {
      const page = this.allResource.value()?.page;
      return {
        number: this.page(),
        totalElements: page?.totalElements ?? 0,
        totalPages: page?.totalPages ?? 0,
      };
    }
    return {
      number: this.page(),
      totalElements: this.mineFiltered().length,
      totalPages: Math.ceil(this.mineFiltered().length / PATIENTS_PAGE_SIZE),
    };
  });
  private readonly mineIds = computed(
    () => new Set((this.mineResource.value() ?? []).map((patient) => patient.id)),
  );
  private readonly activeResource = computed(() =>
    this.scope() === 'all' ? this.allResource : this.mineResource,
  );
  readonly isLoading = computed(() => this.activeResource().isLoading());
  readonly errorMessage = resourceError(
    { error: () => this.activeResource().error() },
    'No se pudieron cargar los pacientes',
  );

  reload(): void {
    this.allResource.reload();
    this.mineResource.reload();
  }

  setScope(scope: PatientScope): void {
    this._scope.set(scope);
    this.page.set(0);
  }

  setSearch(search: string): void {
    this._search.set(search);
  }

  setShowInactive(showInactive: boolean): void {
    this._showInactive.set(showInactive);
    this.page.set(0);
  }

  setPage(page: number): void {
    this.page.set(page);
  }

  isMine(patient: Patient): boolean {
    return this.mineIds().has(patient.id);
  }

  save(patient: Patient | undefined, request: PatientRequest): Observable<Patient> {
    const save$ = patient
      ? this.patientsApi.update(patient.id, request)
      : this.patientsApi.create(request);
    return save$.pipe(
      switchMap((saved) =>
        !patient && this.isDoctor
          ? this.staffApi.assignPatient({ patientId: saved.id, doctorId: this.userId }).pipe(
              map(() => saved),
              catchError((error) => {
                this.notifications.error(
                  'Paciente creado, pero no se pudo asignar: ' + apiErrorMessage(error),
                );
                return of(saved);
              }),
            )
          : of(saved),
      ),
      tap(() => this.notifications.success(patient ? 'Paciente actualizado' : 'Paciente creado')),
      finalize(() => this.reload()),
    );
  }

  deactivate(patient: Patient): Observable<unknown> {
    return this.confirm
      .confirmThen(
        {
          title: 'Desactivar paciente',
          message: `${patient.name} ya no podrá agendar nuevas citas.`,
          confirmLabel: 'Desactivar',
          danger: true,
        },
        () => this.patientsApi.deactivate(patient.id),
        'Paciente desactivado',
        'No se pudo desactivar el paciente',
      )
      .pipe(tap(() => this.reload()));
  }

  activate(patient: Patient): Observable<unknown> {
    return this.confirm
      .confirmThen(
        {
          title: 'Reactivar paciente',
          message: `¿Reactivar a ${patient.name}?`,
          confirmLabel: 'Reactivar',
        },
        () => this.patientsApi.activate(patient.id),
        'Paciente reactivado',
        'No se pudo reactivar el paciente',
      )
      .pipe(tap(() => this.reload()));
  }

  toggleMine(patient: Patient): Observable<unknown> {
    const request = { patientId: patient.id, doctorId: this.userId };
    const mine = this.isMine(patient);
    return (
      mine ? this.staffApi.removePatient(request) : this.staffApi.assignPatient(request)
    ).pipe(
      tap(() => {
        this.notifications.success(
          mine ? 'Paciente quitado de tu lista' : 'Paciente agregado a tu lista',
        );
        this.reload();
      }),
      catchError((error) => {
        this.notifications.error(apiErrorMessage(error));
        return EMPTY;
      }),
    );
  }
}
