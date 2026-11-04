import { ResourceRef, computed, inject, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface ClinicScopedResource<T> {
  readonly resource: ResourceRef<T | undefined>;
  load(): void;
  reload(): void;
  invalidate(): void;
}

export function clinicScopedResource<T>(stream: () => Observable<T>): ClinicScopedResource<T> {
  const auth = inject(AuthService);
  const clinicId = computed(() => auth.session()?.clinicId);
  const requested = linkedSignal({ source: clinicId, computation: () => false });
  const version = linkedSignal({ source: clinicId, computation: () => 0 });

  const resource = rxResource({
    params: () => {
      const id = clinicId();
      return requested() && id !== undefined ? { clinicId: id, version: version() } : undefined;
    },
    stream,
  });

  return {
    resource,
    load: () => {
      if (resource.status() === 'error') resource.reload();
      else requested.set(true);
    },
    reload: () => {
      resource.reload();
    },
    invalidate: () => {
      if (!requested()) return;
      if (resource.isLoading()) version.update((v) => v + 1);
      else resource.reload();
    },
  };
}
