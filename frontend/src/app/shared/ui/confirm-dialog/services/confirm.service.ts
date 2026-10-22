import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { labelledByFrame } from '../../dialog-frame/dialog-frame.component';
import { EMPTY, Observable, catchError, filter, map, switchMap, tap } from 'rxjs';
import { apiErrorMessage } from '../../../../core/http/api-error';
import { NotificationService } from '../../../services/notification.service';
import { ConfirmDialogComponent, ConfirmOptions } from '../confirm-dialog.component';

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);
  private readonly notifications = inject(NotificationService);

  ask(options: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open<boolean>(ConfirmDialogComponent, { ...labelledByFrame(), data: options })
      .closed.pipe(map((result) => result === true));
  }

  confirmThen<T>(
    options: ConfirmOptions,
    action: () => Observable<T>,
    successMessage: string,
    errorFallback?: string,
  ): Observable<T> {
    return this.ask(options).pipe(
      filter(Boolean),
      switchMap(() => action()),
      tap(() => this.notifications.success(successMessage)),
      catchError((error) => {
        this.notifications.error(apiErrorMessage(error, errorFallback));
        return EMPTY;
      }),
    );
  }
}
