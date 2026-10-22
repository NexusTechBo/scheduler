import { Injectable, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { labelledByFrame } from '../../dialog-frame/dialog-frame.component';
import { Observable, map } from 'rxjs';
import { ConfirmDialogComponent, ConfirmOptions } from '../confirm-dialog.component';

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  ask(options: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open<boolean>(ConfirmDialogComponent, { ...labelledByFrame(), data: options })
      .closed.pipe(map((result) => result === true));
  }
}
