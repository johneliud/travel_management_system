import { Injectable, inject, signal, computed } from '@angular/core';
import { ModalService } from '../modal/modal.service';
import { AuthModalShell } from '../../shared/components/auth-modal/auth-modal-shell';
import type { AuthModalView } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthModalService {
  private readonly modalService = inject(ModalService);

  private readonly _currentView = signal<AuthModalView>('login');
  private readonly _userEmail = signal('');
  private _redirectUrl: string | null = null;
  private _pendingOtp: string | null = null;

  readonly currentView = this._currentView.asReadonly();
  readonly userEmail = this._userEmail.asReadonly();
  readonly redirectUrl = () => this._redirectUrl;

  readonly viewTitle = computed(() => {
    switch (this._currentView()) {
      case 'login': return 'Welcome back';
      case 'register': return 'Create your account';
      case 'verify-email': return 'Verify your email';
      case 'forgot-password': return 'Reset your password';
      case 'reset-password': return 'Set new password';
      case 'change-password': return 'Change password';
    }
  });

  readonly viewSubtitle = computed(() => {
    switch (this._currentView()) {
      case 'login': return 'Sign in to access your travel dashboard';
      case 'register': return 'Start planning your next adventure';
      case 'verify-email': return 'Enter the 6-digit code sent to your email';
      case 'forgot-password': return "We'll send you a code to reset your password";
      case 'reset-password': return 'Choose a strong new password';
      case 'change-password': return 'Update your account password';
    }
  });

  open(view: AuthModalView = 'login', options?: { redirectUrl?: string; dismissible?: boolean }): void {
    this._currentView.set(view);
    if (options?.redirectUrl) {
      this._redirectUrl = options.redirectUrl;
    }
    this.modalService.open({
      component: AuthModalShell,
      ariaLabel: this.getAriaLabel(view),
      dismissible: options?.dismissible ?? false,
    });
  }

  consumeRedirectUrl(): string | null {
    const url = this._redirectUrl;
    this._redirectUrl = null;
    return url;
  }

  setPendingOtp(otp: string): void {
    this._pendingOtp = otp;
  }

  consumePendingOtp(): string | null {
    const otp = this._pendingOtp;
    this._pendingOtp = null;
    return otp;
  }

  switchView(view: AuthModalView): void {
    this._currentView.set(view);
  }

  setUserEmail(email: string): void {
    this._userEmail.set(email);
  }

  close(): void {
    this.modalService.close();
  }

  private getAriaLabel(view: AuthModalView): string {
    switch (view) {
      case 'login': return 'Log in';
      case 'register': return 'Register';
      case 'verify-email': return 'Verify email';
      case 'forgot-password': return 'Forgot password';
      case 'reset-password': return 'Reset password';
      case 'change-password': return 'Change password';
    }
  }
}
