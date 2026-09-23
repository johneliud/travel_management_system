import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-forgot-password-view',
  imports: [LucideLoaderCircle],
  templateUrl: './forgot-password-view.html',
})
export class ForgotPasswordView {
  private readonly authModal = inject(AuthModalService);

  readonly email = signal('');
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  markTouched(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
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
    this.error.set('');

    try {
      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ email: this.email() }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Failed to send reset code');
        return;
      }

      this.authModal.setUserEmail(this.email());
      this.success.set('Reset code sent! Check your inbox.');
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
