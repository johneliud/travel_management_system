import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-verify-email-view',
  imports: [FormsModule, LucideLoaderCircle],
  templateUrl: './verify-email-view.html',
})
export class VerifyEmailView {
  private readonly authModal = inject(AuthModalService);
  private readonly session = inject(SessionService);

  readonly otp = signal('');
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly userEmail = this.authModal.userEmail;

  async onSubmit(): Promise<void> {
    this.loading.set(true);
    this.error.set('');

    try {
      const response = await fetch('/api/auth/verify-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ otp: this.otp() }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Verification failed');
        return;
      }

      this.session.updateUser({ emailVerified: true });
      this.success.set('Email verified successfully!');
      setTimeout(() => this.authModal.close(), 1500);
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
