import { TestBed } from '@angular/core/testing';
import type { ComponentFixture } from '@angular/core/testing';
import { ForgotPasswordView } from './forgot-password-view';
import { AuthModalService } from '../../../core/auth/auth-modal.service';
import { ENVIRONMENT } from '../../../core/services/environment.token';

function jsonResponse(body: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
}

describe('ForgotPasswordView', () => {
  let component: ForgotPasswordView;
  let fixture: ComponentFixture<ForgotPasswordView>;
  let authModalSpy: {
    setUserEmail: ReturnType<typeof vi.fn>;
    setPendingOtp: ReturnType<typeof vi.fn>;
    switchView: ReturnType<typeof vi.fn>;
  };
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authModalSpy = {
      setUserEmail: vi.fn(),
      setPendingOtp: vi.fn(),
      switchView: vi.fn(),
    };
    fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordView],
      providers: [
        { provide: AuthModalService, useValue: authModalSpy },
        {
          provide: ENVIRONMENT,
          useValue: { production: true, apiBaseUrl: 'http://localhost:8080' },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('generic messaging', () => {
    it('should switch to reset-password view after successful request', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse({
          message: 'If this email is registered, a reset code has been sent.',
          verificationOtp: null,
        }),
      );
      component.email.set('user@example.com');

      await component.onSubmit();

      expect(authModalSpy.setUserEmail).toHaveBeenCalledWith('user@example.com');
      expect(authModalSpy.switchView).toHaveBeenCalledWith('reset-password');
    });

    it('should show network error on failure', async () => {
      fetchSpy.mockRejectedValueOnce(new Error('Network error'));
      component.email.set('user@example.com');

      await component.onSubmit();

      expect(component.error()).toBe('Network error. Please try again.');
      expect(authModalSpy.switchView).not.toHaveBeenCalled();
    });
  });
});
