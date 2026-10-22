import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { resourceError } from '../../../core/http/api-error';
import { ProfileUpdateRequest, Staff } from '../../../core/models/api.models';
import { NotificationService } from '../../../shared/services/notification.service';

@Injectable()
export class ProfileService {
  private readonly staffApi = inject(StaffApi);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  readonly profile = this.auth.profile;
  readonly isLoading = this.auth.profileLoading;
  readonly errorMessage = resourceError(
    { error: this.auth.profileError },
    'No se pudo cargar tu perfil',
  );

  reload(): void {
    this.auth.reloadProfile();
  }

  update(request: ProfileUpdateRequest): Observable<Staff> {
    return this.staffApi.updateSelf(request).pipe(
      tap((profile) => {
        this.notifications.success('Perfil actualizado');
        this.auth.setProfile(profile);
      }),
    );
  }
}
