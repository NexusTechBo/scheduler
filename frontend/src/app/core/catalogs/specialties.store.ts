import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { CatalogsApi } from '../api/catalogs.api';
import { resourceError } from '../http/api-error';
import { Specialty, SpecialtyRequest } from '../models/api.models';
import { LOAD_ERROR } from '../utils/messages.util';
import { byName } from '../utils/sort.util';
import { clinicScopedResource } from './clinic-scoped-resource';

@Injectable({ providedIn: 'root' })
export class SpecialtiesStore {
  private readonly api = inject(CatalogsApi);
  private readonly cache = clinicScopedResource(() => this.api.specialties());
  private readonly resource = this.cache.resource;

  readonly specialties = computed(() => (this.resource.hasValue() ? this.resource.value() : []));
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, LOAD_ERROR.specialties);

  load(): void {
    this.cache.load();
  }

  reload(): void {
    this.cache.reload();
  }

  create(request: SpecialtyRequest): Observable<Specialty> {
    return this.api.createSpecialty(request).pipe(tap((specialty) => this.add(specialty)));
  }

  private add(specialty: Specialty): void {
    if (this.resource.hasValue() && !this.resource.isLoading()) {
      this.resource.update((list) =>
        [...list.filter((s) => s.id !== specialty.id), specialty].sort(byName),
      );
    } else {
      this.cache.invalidate();
    }
  }
}
