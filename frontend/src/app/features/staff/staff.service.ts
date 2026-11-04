import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, finalize, tap } from 'rxjs';
import { StaffApi } from '../../core/api/staff.api';
import { DoctorsStore } from '../../core/catalogs/doctors.store';
import { SpecialtiesStore } from '../../core/catalogs/specialties.store';
import { resourceError } from '../../core/http/api-error';
import {
  Staff,
  StaffFilters,
  StaffRegisterRequest,
  StaffUpdateRequest,
} from '../../core/models/api.models';
import { ACTION_LABEL } from '../../core/utils/messages.util';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { StaffRoleFilter } from './staff.model';

const PAGE_SIZE = 12;

@Injectable()
export class StaffService {
  private readonly staffApi = inject(StaffApi);
  private readonly specialtiesStore = inject(SpecialtiesStore);
  private readonly doctorsStore = inject(DoctorsStore);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  private readonly filters = signal<StaffFilters>({
    page: 0,
    size: PAGE_SIZE,
    sort: 'id',
    isActive: true,
  });

  private readonly staffResource = rxResource({
    params: () => this.filters(),
    stream: ({ params }) => this.staffApi.list(params),
  });

  readonly staff = computed(() =>
    this.staffResource.hasValue() ? this.staffResource.value().content : [],
  );
  readonly specialties = this.specialtiesStore.specialties;
  readonly pagination = computed(() =>
    this.staffResource.hasValue() ? this.staffResource.value().page : null,
  );
  readonly roleFilter = computed<StaffRoleFilter>(() => this.filters().role ?? 'all');
  readonly onlyActive = computed(() => this.filters().isActive === true);
  readonly isLoading = this.staffResource.isLoading;
  readonly errorMessage = resourceError(this.staffResource, 'No se pudo cargar el personal');

  constructor() {
    this.specialtiesStore.load();
  }

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
          confirmLabel: ACTION_LABEL.deactivate,
          danger: true,
        },
        () => this.staffApi.deactivate(staff.id),
        'Cuenta desactivada',
        'No se pudo desactivar la cuenta',
      )
      .pipe(tap(() => this.afterChange()));
  }

  activate(staff: Staff): Observable<unknown> {
    return this.confirm
      .confirmThen(
        {
          title: 'Reactivar cuenta',
          message: `¿Reactivar a ${staff.name}?`,
          confirmLabel: ACTION_LABEL.reactivate,
        },
        () => this.staffApi.activate(staff.id),
        'Cuenta reactivada',
        'No se pudo reactivar la cuenta',
      )
      .pipe(tap(() => this.afterChange()));
  }

  private afterSave(request: Observable<Staff>, message: string): Observable<Staff> {
    return request.pipe(
      tap(() => {
        this.notifications.success(message);
        this.doctorsStore.invalidate();
      }),
      finalize(() => this.reload()),
    );
  }

  private afterChange(): void {
    this.reload();
    this.doctorsStore.invalidate();
  }

  private updateFilters(patch: Partial<StaffFilters>): void {
    this.filters.update((current) => ({ ...current, page: 0, ...patch }));
  }
}
