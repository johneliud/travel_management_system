import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _notifications = signal<NotificationItem[]>([]);
  private nextId = 0;

  readonly notifications = this._notifications.asReadonly();

  show(type: NotificationType, message: string, durationMs = 5000): void {
    if (!message) return;

    const id = ++this.nextId;
    this._notifications.update((list) => [...list, { id, type, message }]);

    setTimeout(() => this.dismiss(id), durationMs);
  }

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  dismiss(id: number): void {
    this._notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
