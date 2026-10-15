import { computed, inject, Injectable, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, of } from 'rxjs';

import { PatientsApi } from '../../../core/api/patients.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Patient } from '../../../core/models/api.models';

export const PATIENTS_PAGE_SIZE = 12;
/**
 * The backend has no patient search endpoint, so the list is fetched once per scope and filtered client-side.
 * Fine for an MVP clinic size; move to a server-side filter if clinics grow past this.
 */
const MAX_PATIENTS = 1000;

export type PatientScope = 'mine' | 'all';

export interface PatientsQuery {
  scope: PatientScope;
  search: string;
  showInactive: boolean;
  page: number;
}

export const getDefaultPatientsQuery = (isDoctor: boolean): PatientsQuery => ({
  scope: isDoctor ? 'mine' : 'all',
  search: '',
  showInactive: false,
  page: 0
});

/**
 * DOCTOR / ASSISTANT: patients of the clinic.
 * "all" = `GET /api/patients`, "mine" = the doctor's assigned list (`GET /api/personal/{id}/patients`).
 * Only `scope` triggers a request; search, inactive toggle and page are applied locally.
 */
@Injectable()
export class PatientsResourceService {
  private readonly _patientsApi = inject(PatientsApi);
  private readonly _staffApi = inject(StaffApi);
  private readonly _auth = inject(AuthService);

  public readonly isDoctor = this._auth.hasRole('DOCTOR');

  public filters = signal<PatientsQuery>(getDefaultPatientsQuery(this.isDoctor));
  public scope = computed(() => this.filters().scope);
  public search = computed(() => this.filters().search);
  public showInactive = computed(() => this.filters().showInactive);

  private _patientsResource = rxResource({
    request: () => this.scope(),
    loader: ({ request }) =>
      request === 'mine'
        ? this._staffApi.patientsOf(this._auth.userId()!)
        : this._patientsApi.list({ size: MAX_PATIENTS, sort: 'id' }).pipe(map((page) => page.content))
  });

  /** Ids of the doctor's own patients, to offer "assign to me" / "remove" in the "all" tab. */
  private _mineResource = rxResource({
    loader: () => (this.isDoctor ? this._staffApi.patientsOf(this._auth.userId()!) : of([] as Patient[]))
  });

  public allPatients = linkedSignal(() => this._patientsResource.value() ?? ([] as Patient[]));
  private _filtered = computed(() => {
    const { search, showInactive } = this.filters();
    const term = search.trim().toLowerCase();
    return this.allPatients()
      .filter((patient) => showInactive || patient.active)
      .filter((patient) => !term || `${patient.name} ${patient.email} ${patient.phoneNumber ?? ''}`.toLowerCase().includes(term));
  });
  public patients = computed(() => {
    const start = this.filters().page * PATIENTS_PAGE_SIZE;
    return this._filtered().slice(start, start + PATIENTS_PAGE_SIZE);
  });
  public pagination = computed(() => ({
    number: this.filters().page,
    size: PATIENTS_PAGE_SIZE,
    totalElements: this._filtered().length,
    totalPages: Math.ceil(this._filtered().length / PATIENTS_PAGE_SIZE)
  }));
  public mineIds = computed(() => new Set((this._mineResource.value() ?? []).map((patient) => patient.id)));
  public isLoading = this._patientsResource.isLoading;
  public isError = computed(() => !!this._patientsResource.error());
  public errorMessage = computed(() => {
    const error = this._patientsResource.error();
    return error ? apiErrorMessage(error, 'No se pudieron cargar los pacientes') : '';
  });

  public reloadPatients = () => {
    this._patientsResource.reload();
    this._mineResource.reload();
  };

  public updateFilters = (patch: Partial<PatientsQuery>) => this.filters.update((current) => ({ ...current, page: 0, ...patch }));

  public setScope = (scope: PatientScope) => this.updateFilters({ scope });

  public setSearch = (search: string) => this.updateFilters({ search });

  public setShowInactive = (showInactive: boolean) => this.updateFilters({ showInactive });

  public setPage = (page: number) => this.filters.update((current) => ({ ...current, page }));

  public isMine = (patient: Patient) => this.mineIds().has(patient.id);

  public resetResource = () => {
    this.filters.set(getDefaultPatientsQuery(this.isDoctor));
    this.allPatients.set([]);
  };
}
