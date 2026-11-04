import { Injectable, inject, signal } from '@angular/core';
import { EMPTY, Observable, catchError, defer, finalize, tap } from 'rxjs';
import { SpecialtiesStore } from '../../core/catalogs/specialties.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { Specialty } from '../../core/models/api.models';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class SpecialtiesService {
  private readonly store = inject(SpecialtiesStore);
  private readonly notifications = inject(NotificationService);

  readonly specialties = this.store.specialties;
  readonly isLoading = this.store.isLoading;
  readonly errorMessage = this.store.errorMessage;
  private readonly _saving = signal(false);
  readonly saving = this._saving.asReadonly();

  constructor() {
    this.store.load();
  }

  reload(): void {
    this.store.reload();
  }

  create(name: string): Observable<Specialty> {
    return defer(() => {
      this._saving.set(true);
      return this.store.create({ name });
    }).pipe(
      tap(() => this.notifications.success(`Especialidad "${name}" creada`)),
      catchError((error) => {
        this.notifications.error(apiErrorMessage(error, 'No se pudo crear la especialidad'));
        return EMPTY;
      }),
      finalize(() => this._saving.set(false)),
    );
  }
}
