import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { LucideEye, LucideEyeOff, LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-reset-password-view',
  imports: [LucideEye, LucideEyeOff, LucideLoaderCircle],
  templateUrl: './reset-password-view.html',
})
export class ResetPasswordView {
  private readonly authModal = inject(AuthModalService);

  readonly otp = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  readonly userEmail = this.authModal.userEmail;

  markTouched(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
  }

  isTouched(field: string): boolean {
    return this.touchedFields().has(field);
  }

  get otpError(): string | null {
    const val = this.otp();
    if (!this.isTouched('otp') || !val) return null;
    if (!/^\d{6}$/.test(val)) return 'Code must be exactly 6 digits';
    return null;
  }

  get passwordError(): string | null {
    const val = this.newPassword();
    if (!this.isTouched('newPassword') || !val) return null;
    if (val.length < 8) return 'Password must be at least 8 characters';
    return null;
  }

  get confirmPasswordError(): string | null {
    const val = this.confirmPassword();
    if (!this.isTouched('confirmPassword') || !val) return null;
    if (val !== this.newPassword()) return 'Passwords do not match';
    return null;
  }

  get formValid(): boolean {
    return this.otpError === null
      && this.passwordError === null
      && this.confirmPasswordError === null
      && this.otp().length > 0
      && this.newPassword().length > 0
      && this.confirmPassword().length > 0;
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  async onSubmit(): Promise<void> {
    if (!this.formValid) return;

    this.loading.set(true);
    this.error.set('');

    try {
      const response = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({
          currentPassword: '',
          newPassword: this.newPassword(),
          otp: this.otp(),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Password reset failed');
        return;
      }

      this.success.set('Password reset successful! You can now log in.');
      setTimeout(() => this.authModal.switchView('login'), 2000);
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
