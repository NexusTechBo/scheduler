import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error';

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
}

const AUTO_DISMISS_MS = 4000;

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly _notifications = signal<Notification[]>([]);
  public readonly notifications = this._notifications.asReadonly();

  private _nextId = 0;

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  dismiss(id: number): void {
    this._notifications.update((list) => list.filter((n) => n.id !== id));
  }

  private push(type: NotificationType, message: string): void {
    const id = this._nextId++;
    this._notifications.update((list) => [...list, { id, type, message }]);
    setTimeout(() => this.dismiss(id), AUTO_DISMISS_MS);
  }
}
