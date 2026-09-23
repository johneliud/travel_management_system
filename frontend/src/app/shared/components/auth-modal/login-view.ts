import { Component, signal, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-login-view',
  imports: [IconComponent],
  templateUrl: './login-view.html',
})
export class LoginView {
  private readonly authModal = inject(AuthModalService);
  private readonly session = inject(SessionService);

  readonly email = signal('');
  readonly password = signal('');
  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  markTouched(field: string): void {
    this.touchedFields.update(fields => new Set(fields).add(field));
  }

  isTouched(field: string): boolean {
    return this.touchedFields().has(field);
  }

  get emailError(): string | null {
    const val = this.email().trim();
    if (!this.isTouched('email') || !val) return null;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return 'Please enter a valid email address';
    return null;
  }

  get passwordError(): string | null {
    const val = this.password().trim();
    if (!this.isTouched('password') || !val) return null;
    if (val.length < 8) return 'Password must be at least 8 characters';
    return null;
  }

  get formValid(): boolean {
    return this.emailError === null
      && this.passwordError === null
      && this.email().length > 0
      && this.password().length > 0;
  }

  switchView(view: 'register' | 'forgot-password'): void {
    this.error.set('');
    this.authModal.switchView(view);
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  async onSubmit(): Promise<void> {
    if (!this.formValid) return;

    this.loading.set(true);
    this.error.set('');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ email: this.email(), password: this.password() }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        this.error.set(body.message || 'Login failed');
        return;
      }

      const data = await response.json();
      this.session.setSession(data.accessToken, data.refreshToken, {
        id: data.userId,
        email: data.email,
        emailVerified: data.emailVerified ?? false,
        roles: data.roles ?? [],
      });
      this.authModal.close();
    } catch {
      this.error.set('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }
}
