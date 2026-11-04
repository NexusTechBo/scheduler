import { Injectable, computed, inject, isDevMode } from '@angular/core';
import { map } from 'rxjs';
import { StaffApi } from '../api/staff.api';
import { resourceError } from '../http/api-error';
import { LOAD_ERROR } from '../utils/messages.util';
import { clinicScopedResource } from './clinic-scoped-resource';

const MAX_DOCTORS = 500;

@Injectable({ providedIn: 'root' })
export class DoctorsStore {
  private readonly api = inject(StaffApi);
  private readonly cache = clinicScopedResource(() =>
    this.api.doctors({ isActive: true, size: MAX_DOCTORS }).pipe(
      map(({ content, page }) => {
        if (isDevMode() && page.totalElements > content.length) {
          console.warn(`DoctorsStore: showing ${content.length} of ${page.totalElements} doctors`);
        }
        return content;
      }),
    ),
  );
  private readonly resource = this.cache.resource;

  readonly doctors = computed(() => (this.resource.hasValue() ? this.resource.value() : []));
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, LOAD_ERROR.doctors);

  load(): void {
    this.cache.load();
  }

  reload(): void {
    this.cache.reload();
  }

  invalidate(): void {
    this.cache.invalidate();
  }
}
