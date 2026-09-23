import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { NotificationService } from '../../../core/notification/notification.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-forgot-password-view',
  imports: [],
  templateUrl: './forgot-password-view.html',
})
export class ForgotPasswordView {
  private readonly authModal = inject(AuthModalService);
  private readonly notification = inject(NotificationService);

  readonly email = signal('');
  readonly loading = signal(false);
  readonly error = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  private setError(message: string): void {
    this.error.set(message);
    if (message) this.notification.error(message);
  }

  markTouched(field: string): void {
    this.touchedFields.update((fields) => new Set(fields).add(field));
  }

  isTouched(field: string): boolean {
    return this.touchedFields().has(field);
  }

  get emailError(): string | null {
    const val = this.email();
    if (!this.isTouched('email') || !val) return null;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return 'Please enter a valid email address';
    return null;
  }

  get formValid(): boolean {
    return this.emailError === null && this.email().length > 0;
  }

  async onSubmit(): Promise<void> {
    if (!this.formValid) return;

    this.loading.set(true);
    this.setError('');

    try {
      const response = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ email: this.email() }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.setError(body.message || 'Failed to send reset code');
        return;
      }

      const data = await response.json();
      this.authModal.setUserEmail(this.email());

      if (!environment.production && data.verificationOtp) {
        this.authModal.setPendingOtp(data.verificationOtp);
      }

      this.authModal.switchView('reset-password');
    } catch {
      this.setError('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
