import { Component, inject } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { LucideX } from '@lucide/angular';
import { LoginView } from './login-view';
import { RegisterView } from './register-view';
import { VerifyEmailView } from './verify-email-view';
import { ForgotPasswordView } from './forgot-password-view';
import { ResetPasswordView } from './reset-password-view';
import { ChangePasswordView } from './change-password-view';

@Component({
  selector: 'app-auth-modal-shell',
  imports: [
    LucideX,
    LoginView,
    RegisterView,
    VerifyEmailView,
    ForgotPasswordView,
    ResetPasswordView,
    ChangePasswordView,
  ],
  templateUrl: './auth-modal-shell.html',
})
export class AuthModalShell {
  private readonly authModal = inject(AuthModalService);

  readonly currentView = this.authModal.currentView;
  readonly viewTitle = this.authModal.viewTitle;
  readonly viewSubtitle = this.authModal.viewSubtitle;
  readonly currentYear = new Date().getFullYear();

  switchView(view: 'login' | 'register'): void {
    this.authModal.switchView(view);
  }

  close(): void {
    this.authModal.close();
  }
}
