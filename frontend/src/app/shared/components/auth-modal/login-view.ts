import { Component, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { AuthService } from '../../../core/auth/auth.service';
import { LucideEye, LucideEyeOff, LucideLoaderCircle } from '@lucide/angular';

@Component({
  selector: 'app-login-view',
  imports: [LucideEye, LucideEyeOff, LucideLoaderCircle],
  templateUrl: './login-view.html',
})
export class LoginView {
  private readonly authModal = inject(AuthModalService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly email = signal('');
  readonly password = signal('');
  readonly showPassword = signal(false);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly touchedFields = signal<Set<string>>(new Set());

  markTouched(field: string): void {
    this.touchedFields.update((fields) => new Set(fields).add(field));
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
    return (
      this.emailError === null &&
      this.passwordError === null &&
      this.email().length > 0 &&
      this.password().length > 0
    );
  }

  switchView(view: 'register' | 'forgot-password'): void {
    this.error.set('');
    this.authModal.switchView(view);
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  async onSubmit(): Promise<void> {
    if (!this.formValid) return;

    this.loading.set(true);
    this.error.set('');

    try {
      await this.auth.login(this.email(), this.password());
      this.authModal.close();

      const redirectUrl = this.authModal.consumeRedirectUrl();
      if (redirectUrl) {
        this.router.navigateByUrl(redirectUrl);
      }
    } catch (e) {
      this.error.set(e instanceof Error ? e.message : 'Login failed');
    } finally {
      this.loading.set(false);
    }
  }
}
