import { TestBed } from '@angular/core/testing';
import type { ComponentFixture } from '@angular/core/testing';
import { ResetPasswordView } from './reset-password-view';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { ENVIRONMENT } from '../../../core/services/environment.token';

function jsonResponse(body: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('ResetPasswordView', () => {
  let component: ResetPasswordView;
  let fixture: ComponentFixture<ResetPasswordView>;
  let authModalSpy: {
    switchView: ReturnType<typeof vi.fn>;
    userEmail: ReturnType<typeof vi.fn>;
    consumePendingOtp: ReturnType<typeof vi.fn>;
  };
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authModalSpy = {
      switchView: vi.fn(),
      userEmail: vi.fn().mockReturnValue('test@example.com'),
      consumePendingOtp: vi.fn().mockReturnValue(null),
    };
    fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    await TestBed.configureTestingModule({
      imports: [ResetPasswordView],
      providers: [
        { provide: AuthModalService, useValue: authModalSpy },
        {
          provide: ENVIRONMENT,
          useValue: { production: true, apiBaseUrl: 'http://localhost:8080' },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function fillValidForm(): void {
    component.otp.set('123456');
    component.newPassword.set('NewPass!123');
    component.confirmPassword.set('NewPass!123');
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('successful reset', () => {
    it('should switch to login view after successful reset', async () => {
      fetchSpy.mockResolvedValueOnce(new Response(null, { status: 200 }));
      fillValidForm();

      await component.onSubmit();

      expect(fetchSpy).toHaveBeenCalledWith(
        '/api/auth/reset-password',
        expect.objectContaining({
          body: JSON.stringify({
            email: 'test@example.com',
            otp: '123456',
            newPassword: 'NewPass!123',
          }),
        }),
      );
      expect(authModalSpy.switchView).toHaveBeenCalledWith('login');
    });
  });

  describe('expired OTP', () => {
    it('should show expired-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Verification OTP has expired' },
          { status: 401 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('This code has expired. Please request a new one.');
      expect(component.errorType()).toBe('expired');
      expect(authModalSpy.switchView).not.toHaveBeenCalled();
    });
  });

  describe('incorrect OTP', () => {
    it('should show incorrect-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Invalid verification OTP' },
          { status: 401 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('The code you entered is incorrect. Please try again.');
      expect(component.errorType()).toBe('invalid');
    });
  });

  describe('already used OTP', () => {
    it('should show already-used-specific error message', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'INVALID_VERIFICATION_OTP', message: 'Verification OTP has already been used' },
          { status: 401 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('This code has already been used. Please request a new one.');
      expect(component.errorType()).toBe('used');
    });
  });

  describe('form validation', () => {
    it('should not submit when OTP is incomplete', async () => {
      component.otp.set('12345');
      component.newPassword.set('NewPass!123');
      component.confirmPassword.set('NewPass!123');
      component.markTouched('otp');

      await component.onSubmit();

      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('should not submit when passwords do not match', async () => {
      component.otp.set('123456');
      component.newPassword.set('NewPass!123');
      component.confirmPassword.set('Different!1');
      component.markTouched('confirmPassword');

      await component.onSubmit();

      expect(fetchSpy).not.toHaveBeenCalled();
    });
  });

  describe('network error', () => {
    it('should show network error message', async () => {
      fetchSpy.mockRejectedValueOnce(new Error('Network error'));
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('Network error. Please try again.');
    });
  });
});
