import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, tap } from 'rxjs';
import { AuthApi } from '../../../core/api/auth.api';
import { StaffApi } from '../../../core/api/staff.api';
import { AuthService } from '../../../core/auth/auth.service';
import { resourceError } from '../../../core/http/api-error';
import { ProfileUpdateRequest, Staff } from '../../../core/models/api.models';
import { NotificationService } from '../../../shared/services/notification.service';

@Injectable()
export class ProfileService {
  private readonly authApi = inject(AuthApi);
  private readonly staffApi = inject(StaffApi);
  private readonly notifications = inject(NotificationService);
  private readonly auth = inject(AuthService);

  private readonly resource = rxResource({ loader: () => this.authApi.me() });

  readonly profile = computed(() => (this.resource.hasValue() ? this.resource.value() : undefined));
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, 'No se pudo cargar tu perfil');

  reload(): void {
    this.resource.reload();
  }

  update(request: ProfileUpdateRequest): Observable<Staff> {
    return this.staffApi.updateSelf(request).pipe(
      tap((profile) => {
        this.notifications.success('Perfil actualizado');
        this.resource.set(profile);
        this.auth.setProfile(profile);
      }),
    );
  }
}
