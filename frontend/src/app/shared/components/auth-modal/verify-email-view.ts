import { Component, signal, inject, ViewChildren } from '@angular/core';
import type { OnInit, OnDestroy, QueryList, ElementRef } from '@angular/core';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { NotificationService } from '../../../core/notification/notification.service';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-verify-email-view',
  imports: [],
  templateUrl: './verify-email-view.html',
})
export class VerifyEmailView implements OnInit, OnDestroy {
  private readonly authModal = inject(AuthModalService);
  private readonly session = inject(SessionService);
  private readonly notification = inject(NotificationService);

  @ViewChildren('otpInput') readonly otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  readonly digits = signal<string[]>(['', '', '', '', '', '']);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly errorType = signal<'expired' | 'used' | 'invalid' | null>(null);

  readonly userEmail = this.authModal.userEmail;

  readonly canResend = signal(true);
  readonly cooldown = signal(0);
  private cooldownTimer: ReturnType<typeof setInterval> | null = null;

  private setError(message: string): void {
    this.error.set(message);
    if (message) this.notification.error(message);
  }

  private setSuccess(message: string): void {
    this.success.set(message);
    if (message) this.notification.success(message);
  }

  ngOnInit(): void {
    if (!environment.production) {
      const pendingOtp = this.authModal.consumePendingOtp();
      if (pendingOtp) {
        const chars = pendingOtp.split('').slice(0, 6);
        const filled = [...chars, ...Array(6 - chars.length).fill('')];
        this.digits.set(filled);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
  }

  onInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '').slice(0, 1);

    this.digits.update((d) => {
      const updated = [...d];
      updated[index] = value;
      return updated;
    });

    if (value && index < 5) {
      this.focusInput(index + 1);
    }

    this.error.set('');
    this.errorType.set(null);
  }

  onKeyDown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.digits()[index] && index > 0) {
      this.focusInput(index - 1);
      this.digits.update((d) => {
        const updated = [...d];
        updated[index - 1] = '';
        return updated;
      });
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text') ?? '';
    const chars = pasted.replace(/\D/g, '').split('').slice(0, 6);

    if (chars.length > 0) {
      const filled = [...chars, ...Array(6 - chars.length).fill('')];
      this.digits.set(filled);

      const focusIndex = Math.min(chars.length, 5);
      this.focusInput(focusIndex);
    }
  }

  get otp(): string {
    return this.digits().join('');
  }

  get formValid(): boolean {
    return this.otp.length === 6;
  }

  focusInput(index: number): void {
    const inputs = this.otpInputs?.toArray();
    if (inputs?.[index]) {
      inputs[index].nativeElement.focus();
    }
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
    this.setError('');
    this.errorType.set(null);

    try {
      const response = await fetch('/api/auth/verify-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ otp: this.otp }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        const parsed = this.parseError(body.message || 'Verification failed');
        this.setError(parsed.text);
        this.errorType.set(parsed.type);
        return;
      }

      this.session.updateUser({ emailVerified: true });
      this.setSuccess('Email verified successfully!');
      setTimeout(() => this.authModal.close(), 1500);
    } catch {
      this.setError('Network error. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }

  async onResend(): Promise<void> {
    if (!this.canResend()) return;

    this.canResend.set(false);
    this.setError('');
    this.errorType.set(null);

    try {
      const email = this.userEmail();
      if (!email) return;

      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        if (body.message?.includes('cooldown') || body.message?.includes('wait')) {
          this.setError(body.message);
        } else {
          this.setError(body.message || 'Failed to resend code');
        }
        this.startCooldown(5);
        return;
      }

      const data = await response.json();
      if (!environment.production && data.verificationOtp) {
        const chars = data.verificationOtp.split('').slice(0, 6);
        const filled = [...chars, ...Array(6 - chars.length).fill('')];
        this.digits.set(filled);
      }

      this.setSuccess('A new code has been sent to your email.');
      this.startCooldown(60);
    } catch {
      this.setError('Network error. Please try again.');
      this.startCooldown(5);
    }
  }

  private startCooldown(seconds: number): void {
    this.cooldown.set(seconds);
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
    this.cooldownTimer = setInterval(() => {
      const remaining = this.cooldown() - 1;
      if (remaining <= 0) {
        this.cooldown.set(0);
        this.canResend.set(true);
        if (this.cooldownTimer) {
          clearInterval(this.cooldownTimer);
          this.cooldownTimer = null;
        }
      } else {
        this.cooldown.set(remaining);
      }
    }, 1000);
  }
}
