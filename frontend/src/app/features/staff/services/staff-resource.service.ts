import { computed, inject, Injectable, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { StaffApi } from '../../../core/api/staff.api';
import { apiErrorMessage } from '../../../core/http/api-error';
import { Staff, StaffFilters } from '../../../core/models/api.models';

export const STAFF_PAGE_SIZE = 12;

export const getDefaultStaffFilters = (): StaffFilters => ({ page: 0, size: STAFF_PAGE_SIZE, sort: 'id', isActive: true });

/** ADMINISTRATOR: server state of the clinic staff list (backend `/api/personal`). */
@Injectable()
export class StaffResourceService {
  private readonly _staffApi = inject(StaffApi);

  public filters = signal<StaffFilters>(getDefaultStaffFilters());

  private _staffResource = rxResource({
    request: () => this.filters(),
    loader: ({ request }) => this._staffApi.list(request)
  });

  public staff = linkedSignal(() => this._staffResource.value()?.content ?? ([] as Staff[]));
  public pagination = computed(() => this._staffResource.value()?.page ?? null);
  /** `'all'` or the role id as string, matching the segmented tabs ids. */
  public roleFilter = computed(() => (this.filters().roleId ? String(this.filters().roleId) : 'all'));
  public onlyActive = computed(() => this.filters().isActive === true);
  public isLoading = this._staffResource.isLoading;
  public isError = computed(() => !!this._staffResource.error());
  public errorMessage = computed(() => {
    const error = this._staffResource.error();
    return error ? apiErrorMessage(error, 'No se pudo cargar el personal') : '';
  });
  public reloadStaff = () => this._staffResource.reload();

  public updateFilters = (patch: Partial<StaffFilters>) => this.filters.update((current) => ({ ...current, page: 0, ...patch }));

  public setRole = (role: string) => this.updateFilters({ roleId: role === 'all' ? undefined : Number(role) });

  public setOnlyActive = (onlyActive: boolean) => this.updateFilters({ isActive: onlyActive ? true : undefined });

  public setPage = (page: number) => this.filters.update((current) => ({ ...current, page }));

  public resetResource = () => {
    this.filters.set(getDefaultStaffFilters());
    this.staff.set([]);
  };
}
