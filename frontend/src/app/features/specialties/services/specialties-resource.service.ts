import { computed, inject, Injectable, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CatalogsApi } from '../../../core/api/catalogs.api';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Specialty } from '../../../core/models/api.models';

/** ADMINISTRATOR: specialties catalog (`GET /api/specialties`, not paginated). */
@Injectable()
export class SpecialtiesResourceService {
  private readonly _catalogsApi = inject(CatalogsApi);

  private _specialtiesResource = rxResource({
    loader: () => this._catalogsApi.specialties()
  });

  public specialties = linkedSignal(() => this._specialtiesResource.value() ?? ([] as Specialty[]));
  public isLoading = this._specialtiesResource.isLoading;
  public isError = computed(() => !!this._specialtiesResource.error());
  public errorMessage = computed(() => {
    const error = this._specialtiesResource.error();
    return error ? apiErrorMessage(error, 'No se pudieron cargar las especialidades') : '';
  });
  public reloadSpecialties = () => this._specialtiesResource.reload();
}
