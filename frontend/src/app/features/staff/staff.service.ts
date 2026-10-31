import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, finalize, tap } from 'rxjs';
import { CatalogsApi } from '../../core/api/catalogs.api';
import { StaffApi } from '../../core/api/staff.api';
import { resourceError } from '../../core/http/api-error';
import {
  Staff,
  StaffFilters,
  StaffRegisterRequest,
  StaffUpdateRequest,
} from '../../core/models/api.models';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { StaffRoleFilter } from './staff.model';

const PAGE_SIZE = 12;

@Injectable()
export class StaffService {
  private readonly staffApi = inject(StaffApi);
  private readonly catalogsApi = inject(CatalogsApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  private readonly filters = signal<StaffFilters>({
    page: 0,
    size: PAGE_SIZE,
    sort: 'id',
    isActive: true,
  });

  private readonly staffResource = rxResource({
    request: () => this.filters(),
    loader: ({ request }) => this.staffApi.list(request),
  });
  private readonly specialtiesResource = rxResource({
    loader: () => this.catalogsApi.specialties(),
  });

  readonly staff = computed(() => this.staffResource.value()?.content ?? []);
  readonly specialties = computed(() => this.specialtiesResource.value() ?? []);
  readonly pagination = computed(() => this.staffResource.value()?.page ?? null);
  readonly roleFilter = computed<StaffRoleFilter>(() => this.filters().role ?? 'all');
  readonly onlyActive = computed(() => this.filters().isActive === true);
  readonly isLoading = this.staffResource.isLoading;
  readonly errorMessage = resourceError(this.staffResource, 'No se pudo cargar el personal');

  reload(): void {
    this.staffResource.reload();
  }

  setRole(role: StaffRoleFilter): void {
    this.updateFilters({ role: role === 'all' ? undefined : role });
  }

  setOnlyActive(onlyActive: boolean): void {
    this.updateFilters({ isActive: onlyActive ? true : undefined });
  }

  setPage(page: number): void {
    this.filters.update((current) => ({ ...current, page }));
  }

  specialtyIdOf(name: string | null | undefined): number | null {
    return this.specialties().find((s) => s.name === name)?.id ?? null;
  }

  create(request: StaffRegisterRequest): Observable<Staff> {
    return this.afterSave(this.staffApi.create(request), 'Miembro del personal creado');
  }

  update(id: number, request: StaffUpdateRequest): Observable<Staff> {
    return this.afterSave(this.staffApi.update(id, request), 'Datos actualizados');
  }

  deactivate(staff: Staff): Observable<unknown> {
    return this.confirm
      .confirmThen(
        {
          title: 'Desactivar cuenta',
          message: `${staff.name} dejará de aparecer para nuevas reservas. Sus citas existentes se mantienen.`,
          confirmLabel: 'Desactivar',
          danger: true,
        },
        () => this.staffApi.deactivate(staff.id),
        'Cuenta desactivada',
        'No se pudo desactivar la cuenta',
      )
      .pipe(tap(() => this.reload()));
  }

  activate(staff: Staff): Observable<unknown> {
    return this.confirm
      .confirmThen(
        {
          title: 'Reactivar cuenta',
          message: `¿Reactivar a ${staff.name}?`,
          confirmLabel: 'Reactivar',
        },
        () => this.staffApi.activate(staff.id),
        'Cuenta reactivada',
        'No se pudo reactivar la cuenta',
      )
      .pipe(tap(() => this.reload()));
  }

  private afterSave(request: Observable<Staff>, message: string): Observable<Staff> {
    return request.pipe(
      tap(() => this.notifications.success(message)),
      finalize(() => this.reload()),
    );
  }

  private updateFilters(patch: Partial<StaffFilters>): void {
    this.filters.update((current) => ({ ...current, page: 0, ...patch }));
  }
}
