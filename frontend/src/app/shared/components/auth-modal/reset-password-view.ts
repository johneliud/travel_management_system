import { Component, signal, inject } from '@angular/core';
import type { OnInit } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { environment } from '../../../../environment/environment';
import { LucideEye, LucideEyeOff, LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-reset-password-view',
  imports: [LucideEye, LucideEyeOff, LucideLoaderCircle],
  templateUrl: './reset-password-view.html',
})
export class ResetPasswordView implements OnInit {
  private readonly authModal = inject(AuthModalService);

  readonly otp = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly errorType = signal<'expired' | 'used' | 'invalid' | null>(null);
  readonly touchedFields = signal<Set<string>>(new Set());

  readonly userEmail = this.authModal.userEmail;

  ngOnInit(): void {
    if (!environment.production) {
      const pendingOtp = this.authModal.consumePendingOtp();
      if (pendingOtp) {
        this.otp.set(pendingOtp);
      }
    }
  }

  markTouched(field: string): void {
    this.touchedFields.update((fields) => new Set(fields).add(field));
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
    return (
      this.otpError === null &&
      this.passwordError === null &&
      this.confirmPasswordError === null &&
      this.otp().length > 0 &&
      this.newPassword().length > 0 &&
      this.confirmPassword().length > 0
    );
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((v) => !v);
  }

  parseError(message: string): { text: string; type: 'expired' | 'used' | 'invalid' } {
    if (message.includes('expired')) {
      return { text: 'This code has expired. Please request a new one.', type: 'expired' };
    }
    if (message.includes('already been used')) {
      return { text: 'This code has already been used. Please request a new one.', type: 'used' };
    }
    return { text: 'The code you entered is incorrect. Please try again.', type: 'invalid' };
  }

  async onSubmit(): Promise<void> {
    if (!this.formValid) return;

    this.loading.set(true);
    this.error.set('');
    this.errorType.set(null);

    try {
      const email = this.userEmail();
      if (!email) {
        this.error.set('No email set. Please go back and try again.');
        return;
      }

      const response = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({
          email,
          otp: this.otp(),
          newPassword: this.newPassword(),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        const parsed = this.parseError(body.message || 'Password reset failed');
        this.error.set(parsed.text);
        this.errorType.set(parsed.type);
        return;
      }

      this.authModal.switchView('login');
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
