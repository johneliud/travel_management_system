import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { environment } from '../../../../environment/environment';
import { LucideEye, LucideEyeOff, LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-register-view',
  imports: [LucideEye, LucideEyeOff, LucideLoaderCircle],
  templateUrl: './register-view.html',
})
export class RegisterView {
  private readonly authModal = inject(AuthModalService);
  private readonly session = inject(SessionService);

  readonly firstName = signal('');
  readonly lastName = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly confirmPassword = signal('');
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  markTouched(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
  }

  isTouched(field: string): boolean {
    return this.touchedFields().has(field);
  }

  get firstNameError(): string | null {
    const val = this.firstName();
    if (!this.isTouched('firstName') || !val) return null;
    if (val.trim().length < 1) return 'First name is required';
    if (val.length > 100) return 'First name must be 100 characters or less';
    return null;
  }

  get lastNameError(): string | null {
    const val = this.lastName();
    if (!this.isTouched('lastName') || !val) return null;
    if (val.trim().length < 1) return 'Last name is required';
    if (val.length > 100) return 'Last name must be 100 characters or less';
    return null;
  }

  get emailError(): string | null {
    const val = this.email();
    if (!this.isTouched('email') || !val) return null;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return 'Please enter a valid email address';
    return null;
  }

  get passwordError(): string | null {
    const val = this.password();
    if (!this.isTouched('password') || !val) return null;
    if (val.length < 8) return 'Password must be at least 8 characters';
    return null;
  }

  get confirmPasswordError(): string | null {
    const val = this.confirmPassword();
    if (!this.isTouched('confirmPassword') || !val) return null;
    if (val !== this.password()) return 'Passwords do not match';
    return null;
  }

  get formValid(): boolean {
    return this.firstNameError === null
      && this.lastNameError === null
      && this.emailError === null
      && this.passwordError === null
      && this.confirmPasswordError === null
      && this.firstName().length > 0
      && this.lastName().length > 0
      && this.email().length > 0
      && this.password().length > 0
      && this.confirmPassword().length > 0;
  }

  switchView(view: 'login'): void {
    this.error.set('');
    this.authModal.switchView(view);
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
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({
          firstName: this.firstName(),
          lastName: this.lastName(),
          email: this.email(),
          password: this.password(),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        if (response.status === 409 && body.code === 'EMAIL_ALREADY_IN_USE') {
          this.error.set('An account with this email already exists');
        } else {
          this.error.set(body.message || 'Registration failed');
        }
        return;
      }

      const data = await response.json();
      this.session.setSession('pending', 'pending', {
        id: data.id,
        email: data.email,
        emailVerified: false,
        roles: ['TRAVELER'],
      });
      this.authModal.setUserEmail(data.email);

      if (!environment.production && data.verificationOtp) {
        this.authModal.setPendingOtp(data.verificationOtp);
      }

      this.authModal.switchView('verify-email');
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
