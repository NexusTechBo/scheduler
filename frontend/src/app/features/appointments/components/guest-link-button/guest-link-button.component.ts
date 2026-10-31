import { Clipboard } from '@angular/cdk/clipboard';
import { Dialog } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { AuthService } from '../../../../core/auth/auth.service';
import { ROLES } from '../../../../core/auth/roles';
import { GUEST_BOOKING_PATH } from '../../../../core/navigation/navigation';
import { SUCCESS_MESSAGE } from '../../../../core/utils/messages.util';
import { NotificationService } from '../../../../shared/services/notification.service';
import { labelledByFrame } from '../../../../shared/ui/dialog-frame/dialog-frame.component';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';
import {
  GuestLinkData,
  GuestLinkDialogComponent,
} from '../guest-link-dialog/guest-link-dialog.component';

@Component({
  selector: 'app-guest-link-button',
  imports: [UiIconComponent],
  templateUrl: './guest-link-button.component.html',
  host: { class: 'contents' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuestLinkButtonComponent {
  private readonly auth = inject(AuthService);
  private readonly clipboard = inject(Clipboard);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(Dialog);

  private readonly url = computed(() => {
    const slug = this.auth.clinic()?.slug;
    return slug
      ? `${location.origin}/${GUEST_BOOKING_PATH}?slug=${encodeURIComponent(slug)}`
      : null;
  });
  protected readonly visible = computed(
    () => !!this.url() && this.auth.hasRole(...ROLES.guestLink),
  );

  protected copy(): void {
    const url = this.url();
    if (!url) return;
    if (this.clipboard.copy(url)) {
      this.notifications.success(SUCCESS_MESSAGE.guestLinkCopied);
      return;
    }
    this.dialog.open<void, GuestLinkData>(GuestLinkDialogComponent, {
      ...labelledByFrame(),
      data: { url },
    });
  }
}
