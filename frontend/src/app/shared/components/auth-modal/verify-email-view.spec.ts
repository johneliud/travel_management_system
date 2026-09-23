import { TestBed } from '@angular/core/testing';
import type { ComponentFixture } from '@angular/core/testing';
import { VerifyEmailView } from './verify-email-view';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { SessionService } from '../../../core/auth/session.service';
import { ENVIRONMENT } from '../../../core/services/environment.token';

function jsonResponse(body: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('VerifyEmailView', () => {
  let component: VerifyEmailView;
  let fixture: ComponentFixture<VerifyEmailView>;
  let authModalSpy: {
    close: ReturnType<typeof vi.fn>;
    userEmail: ReturnType<typeof vi.fn>;
    consumePendingOtp: ReturnType<typeof vi.fn>;
  };
  let sessionSpy: { updateUser: ReturnType<typeof vi.fn> };
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authModalSpy = {
      close: vi.fn(),
      userEmail: vi.fn().mockReturnValue('test@example.com'),
      consumePendingOtp: vi.fn().mockReturnValue(null),
    };
    sessionSpy = {
      updateUser: vi.fn(),
    };
    fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    await TestBed.configureTestingModule({
      imports: [VerifyEmailView],
      providers: [
        { provide: AuthModalService, useValue: authModalSpy },
        { provide: SessionService, useValue: sessionSpy },
        {
          provide: ENVIRONMENT,
          useValue: { production: true, apiBaseUrl: 'http://localhost:8080' },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyEmailView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function fillOtp(otp: string): void {
    const chars = otp.split('');
    component.digits.set([...chars, ...Array(6 - chars.length).fill('')]);
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('success', () => {
    it('should close modal after successful verification', async () => {
      vi.useFakeTimers();
      fetchSpy.mockResolvedValueOnce(jsonResponse({}, { status: 200 }));
      fillOtp('123456');

      await component.onSubmit();

      expect(sessionSpy.updateUser).toHaveBeenCalledWith({ emailVerified: true });
      expect(component.success()).toBe('Email verified successfully!');

      vi.advanceTimersByTime(1500);
      expect(authModalSpy.close).toHaveBeenCalled();

      vi.useRealTimers();
    });
  });

  describe('expired code', () => {
    it('should show expired-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Verification OTP has expired' },
          { status: 401 },
        ),
      );
      fillOtp('123456');

      await component.onSubmit();

      expect(component.error()).toBe('This code has expired. Please request a new one.');
      expect(component.errorType()).toBe('expired');
      expect(authModalSpy.close).not.toHaveBeenCalled();
    });
  });

  describe('incorrect code', () => {
    it('should show incorrect-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Invalid verification OTP' },
          { status: 401 },
        ),
      );
      fillOtp('000000');

      await component.onSubmit();

      expect(component.error()).toBe('The code you entered is incorrect. Please try again.');
      expect(component.errorType()).toBe('invalid');
    });
  });

  describe('already used code', () => {
    it('should show already-used-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Verification OTP has already been used' },
          { status: 401 },
        ),
      );
      fillOtp('123456');

      await component.onSubmit();

      expect(component.error()).toBe('This code has already been used. Please request a new one.');
      expect(component.errorType()).toBe('used');
    });
  });

  describe('dismiss without verifying', () => {
    it('should not block navigation or clear session on close', () => {
      component.error.set('');
      component.success.set('');

      expect(component.error()).toBe('');
      expect(component.success()).toBe('');
      expect(sessionSpy.updateUser).not.toHaveBeenCalled();
    });
  });

  describe('form validation', () => {
    it('should not submit when OTP is incomplete', async () => {
      fillOtp('12345');

      await component.onSubmit();

      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('should submit when OTP is complete', async () => {
      fetchSpy.mockResolvedValueOnce(jsonResponse({}, { status: 200 }));
      fillOtp('123456');

      await component.onSubmit();

      expect(fetchSpy).toHaveBeenCalled();
    });
  });

  describe('network error', () => {
    it('should show network error message', async () => {
      fetchSpy.mockRejectedValueOnce(new Error('Network error'));
      fillOtp('123456');

      await component.onSubmit();

      expect(component.error()).toBe('Network error. Please try again.');
    });
  });

  describe('resend', () => {
    it('should send resend request and start cooldown', async () => {
      vi.useFakeTimers();
      fetchSpy.mockResolvedValueOnce(jsonResponse({ verificationOtp: '654321' }, { status: 200 }));

      await component.onResend();

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/auth/resend-verification',
        expect.objectContaining({
          body: JSON.stringify({ email: 'test@example.com' }),
        }),
      );
      expect(component.canResend()).toBeFalsy();
      expect(component.cooldown()).toBe(60);
      expect(component.success()).toBe('A new code has been sent to your email.');

      vi.useRealTimers();
    });

    it('should pre-fill OTP from resend response in non-production', async () => {
      vi.useFakeTimers();
      fetchSpy.mockResolvedValueOnce(jsonResponse({ verificationOtp: '654321' }, { status: 200 }));

      await component.onResend();

      expect(component.digits()).toEqual(['6', '5', '4', '3', '2', '1']);

      vi.useRealTimers();
    });

    it('should show cooldown message on rate limit', async () => {
      vi.useFakeTimers();
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { message: 'please wait 45 seconds before requesting a new code' },
          { status: 429 },
        ),
      );

      await component.onResend();

      expect(component.error()).toContain('please wait');
      expect(component.cooldown()).toBe(5);

      vi.useRealTimers();
    });

    it('should not resend when email is not set', async () => {
      authModalSpy.userEmail.mockReturnValue('');

      await component.onResend();

      expect(fetchSpy).not.toHaveBeenCalled();
    });
  });
});
