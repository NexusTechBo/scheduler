import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { SUCCESS_MESSAGE } from '../../../../core/utils/messages.util';
import { NotificationService } from '../../../../shared/services/notification.service';
import { AlertComponent } from '../../../../shared/ui/alert/alert.component';
import { DialogFrameComponent } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';

export interface GuestLinkData {
  url: string;
}

@Component({
  selector: 'app-guest-link-dialog',
  imports: [AlertComponent, CdkCopyToClipboard, DialogFrameComponent, FormFieldComponent],
  templateUrl: './guest-link-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuestLinkDialogComponent {
  private readonly notifications = inject(NotificationService);
  protected readonly ref = inject(DialogRef);
  protected readonly link = inject<GuestLinkData>(DIALOG_DATA);
  protected readonly copyFailed = signal(false);

  protected onCopied(copied: boolean): void {
    if (!copied) {
      this.copyFailed.set(true);
      return;
    }
    this.notifications.success(SUCCESS_MESSAGE.guestLinkCopied);
    this.ref.close();
  }
}
