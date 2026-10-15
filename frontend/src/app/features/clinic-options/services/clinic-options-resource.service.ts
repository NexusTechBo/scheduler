import { computed, inject, Injectable } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { ClinicsApi } from '../../../core/api/clinics.api';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Clinic } from '../../../core/models/api.models';

/** Public clinics list (`GET /api/clinics`), shown before login because the backend is multi-tenant. */
@Injectable()
export class ClinicOptionsResourceService {
  private readonly _clinicsApi = inject(ClinicsApi);

  private _clinicsResource = rxResource({ loader: () => this._clinicsApi.list() });

  public clinics = computed(() => this._clinicsResource.value() ?? ([] as Clinic[]));
  public isLoading = this._clinicsResource.isLoading;
  public isError = computed(() => !!this._clinicsResource.error());
  public errorMessage = computed(() => {
    const error = this._clinicsResource.error();
    return error ? apiErrorMessage(error, 'No se pudieron cargar las clínicas') : '';
  });
  public reloadClinics = () => this._clinicsResource.reload();
}
