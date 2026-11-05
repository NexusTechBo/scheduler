import { Injectable, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { ROLES } from '../../core/auth/roles';
import { DoctorsStore } from '../../core/catalogs/doctors.store';
import { SpecialtiesStore } from '../../core/catalogs/specialties.store';
import { resourceError } from '../../core/http/api-error';
import { DoctorModeRequest, ProfileUpdateRequest, Staff } from '../../core/models/api.models';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class ProfileService {
  private readonly staffApi = inject(StaffApi);
  private readonly specialtiesStore = inject(SpecialtiesStore);
  private readonly doctorsStore = inject(DoctorsStore);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  readonly me = this.auth.profile;
  readonly isLoading = this.auth.profileLoading;
  readonly specialties = this.specialtiesStore.specialties;
  readonly specialtiesLoading = this.specialtiesStore.isLoading;
  readonly specialtiesErrorMessage = this.specialtiesStore.errorMessage;

  readonly errorMessage = resourceError(
    { error: this.auth.profileError },
    'No se pudo cargar tu perfil',
  );
  readonly canToggleDoctorMode = computed(() => this.auth.hasRole(...ROLES.admin));
  readonly doctorMode = computed(() => this.auth.hasRole('DOCTOR'));

  constructor() {
    if (this.canToggleDoctorMode()) this.specialtiesStore.load();
  }

  reload(): void {
    this.auth.reloadProfile();
  }

  reloadSpecialties(): void {
    this.specialtiesStore.reload();
  }

  update(request: ProfileUpdateRequest): Observable<Staff> {
    return this.staffApi.updateSelf(request).pipe(
      tap((profile) => {
        this.notifications.success('Perfil actualizado');
        this.auth.setProfile(profile);
        if (this.doctorMode()) this.doctorsStore.invalidate();
      }),
    );
  }

  enableDoctorMode(specialtyId: number): Observable<unknown> {
    return this.changeDoctorMode({ enabled: true, specialtyId }).pipe(
      tap(() => this.notifications.success('Ahora también atiendes como doctor')),
    );
  }

  disableDoctorMode(): Observable<unknown> {
    return this.confirm.confirmThen(
      {
        title: 'Dejar de atender como doctor',
        message:
          'Ya no aparecerás como doctor para nuevas citas. Antes debes cancelar tus próximas citas y desactivar tus horarios de atención.',
        confirmLabel: 'Dejar de atender',
        danger: true,
      },
      () => this.changeDoctorMode({ enabled: false, specialtyId: null }),
      'Ya no atiendes como doctor',
      'No se pudo desactivar el modo doctor',
    );
  }

  private changeDoctorMode(request: DoctorModeRequest): Observable<unknown> {
    return this.staffApi.updateDoctorMode(request).pipe(
      tap(({ token }) => {
        this.auth.replaceToken(token);
        this.auth.reloadProfile();
        this.doctorsStore.invalidate();
      }),
    );
  }
}
