import { Component, inject } from '@angular/core';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notification',
  imports: [],
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.scss',
})
export class NotificationComponent {
  private readonly _notificationService = inject(NotificationService);
  protected notifications = this._notificationService.notifications;

  dismiss(id: number): void {
    this._notificationService.dismiss(id);
  }
}
