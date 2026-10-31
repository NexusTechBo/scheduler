import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, defer, finalize, tap } from 'rxjs';
import { CatalogsApi } from '../../core/api/catalogs.api';
import { apiErrorMessage, resourceError } from '../../core/http/api-error';
import { Specialty } from '../../core/models/api.models';
import { LOAD_ERROR } from '../../core/utils/messages.util';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class SpecialtiesService {
  private readonly api = inject(CatalogsApi);
  private readonly notifications = inject(NotificationService);

  private readonly resource = rxResource({
    loader: () => this.api.specialties(),
  });

  readonly specialties = computed(() => this.resource.value() ?? []);
  readonly isLoading = this.resource.isLoading;
  readonly errorMessage = resourceError(this.resource, LOAD_ERROR.specialties);
  private readonly _saving = signal(false);
  readonly saving = this._saving.asReadonly();

  reload(): void {
    this.resource.reload();
  }

  create(name: string): Observable<Specialty> {
    return defer(() => {
      this._saving.set(true);
      return this.api.createSpecialty({ name });
    }).pipe(
      tap(() => {
        this.notifications.success(`Especialidad "${name}" creada`);
        this.reload();
      }),
      catchError((error) => {
        this.notifications.error(apiErrorMessage(error, 'No se pudo crear la especialidad'));
        return EMPTY;
      }),
      finalize(() => this._saving.set(false)),
    );
  }
}
