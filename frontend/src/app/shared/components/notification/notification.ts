import { Component, inject } from '@angular/core';
import { NotificationService } from '../../../core/notification/notification.service';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.html',
})
export class NotificationComponent {
  protected readonly notificationService = inject(NotificationService);

  getContainerClass(type: string): string {
    const base =
      'flex items-start gap-3 p-4 rounded-lg shadow-lg border pointer-events-auto animate-slide-in';
    return type === 'success'
      ? `${base} bg-(--color-success-light) text-(--color-success) border-(--color-success)`
      : `${base} bg-(--color-error-light) text-(--color-error) border-(--color-error)`;
  }
}
