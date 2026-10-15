import { Component, Injectable, inject } from '@angular/core';
import { DIALOG_DATA, Dialog, DialogRef } from '@angular/cdk/dialog';
import { Observable, map } from 'rxjs';
import { DialogFrameComponent } from '../dialog-frame/dialog-frame.component';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [DialogFrameComponent],
  template: `
    <app-dialog-frame [title]="data.title" (closed)="ref.close(false)">
      <p class="text-[15px] leading-relaxed text-gray-600">{{ data.message }}</p>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" (click)="ref.close(false)">{{ data.cancelLabel ?? 'Volver' }}</button>
        <button type="button" class="btn" [class]="data.danger ? 'btn-danger' : 'btn-primary'" (click)="ref.close(true)">
          {{ data.confirmLabel ?? 'Confirmar' }}
        </button>
      </div>
    </app-dialog-frame>
  `
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmOptions>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
}

/** `confirm.ask({...}).subscribe(ok => ...)` — emits once, false when dismissed. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  ask(options: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open<boolean>(ConfirmDialogComponent, { data: options, backdropClass: 'glass-backdrop' })
      .closed.pipe(map((result) => result === true));
  }
}
