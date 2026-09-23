import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { LucideEye, LucideEyeOff, LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-change-password-view',
  imports: [LucideEye, LucideEyeOff, LucideLoaderCircle],
  templateUrl: './change-password-view.html',
})
export class ChangePasswordView {
  private readonly authModal = inject(AuthModalService);
  private readonly session = inject(SessionService);

  readonly currentPassword = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly showCurrentPassword = signal(false);
  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly loading = signal(false);
  readonly requestingOtp = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly otp = signal('');
  readonly otpSent = signal(false);
  readonly touchedFields = signal<Set<string>>(new Set());

  markTouched(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
  }

  isTouched(field: string): boolean {
    return this.touchedFields().has(field);
  }

  get currentPasswordError(): string | null {
    const val = this.currentPassword();
    if (!this.isTouched('currentPassword') || !val) return null;
    if (val.length < 1) return 'Current password is required';
    return null;
  }

  get newPasswordError(): string | null {
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

  get otpError(): string | null {
    const val = this.otp();
    if (!this.isTouched('otp') || !val) return null;
    if (!/^\d{6}$/.test(val)) return 'Code must be exactly 6 digits';
    return null;
  }

  get formValid(): boolean {
    return this.currentPasswordError === null
      && this.newPasswordError === null
      && this.confirmPasswordError === null
      && this.currentPassword().length > 0
      && this.newPassword().length > 0
      && this.confirmPassword().length > 0;
  }

  get submitValid(): boolean {
    return this.formValid && this.otpError === null && this.otp().length > 0;
  }

  close(): void {
    this.authModal.close();
  }

  toggleCurrentPassword(): void {
    this.showCurrentPassword.update(v => !v);
  }

  toggleNewPassword(): void {
    this.showNewPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  async requestOtp(): Promise<void> {
    if (!this.formValid) return;

    this.requestingOtp.set(true);
    this.error.set('');

    try {
      const token = this.session.token();
      const response = await fetch('/api/auth/change-password/request-otp', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Version': '1',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Failed to send OTP');
        return;
      }

      this.otpSent.set(true);
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.requestingOtp.set(false);
    }
  }

  async onSubmit(): Promise<void> {
    if (!this.submitValid) return;

    this.loading.set(true);
    this.error.set('');

    try {
      const token = this.session.token();
      const response = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Version': '1',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          currentPassword: this.currentPassword(),
          newPassword: this.newPassword(),
          otp: this.otp(),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Password change failed');
        return;
      }

      this.success.set('Password changed successfully! Please log in again.');
      this.session.clearSession();
      setTimeout(() => this.authModal.close(), 2000);
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
