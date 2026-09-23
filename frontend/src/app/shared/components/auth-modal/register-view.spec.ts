import { TestBed } from '@angular/core/testing';
import type { ComponentFixture } from '@angular/core/testing';
import { RegisterView } from './register-view';
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

describe('RegisterView', () => {
  let component: RegisterView;
  let fixture: ComponentFixture<RegisterView>;
  let authModalSpy: { switchView: ReturnType<typeof vi.fn>; setUserEmail: ReturnType<typeof vi.fn>; setPendingOtp: ReturnType<typeof vi.fn> };
  let sessionSpy: { setSession: ReturnType<typeof vi.fn> };
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    authModalSpy = {
      switchView: vi.fn(),
      setUserEmail: vi.fn(),
      setPendingOtp: vi.fn(),
    };
    sessionSpy = {
      setSession: vi.fn(),
    };
    fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    await TestBed.configureTestingModule({
      imports: [RegisterView],
      providers: [
        { provide: AuthModalService, useValue: authModalSpy },
        { provide: SessionService, useValue: sessionSpy },
        { provide: ENVIRONMENT, useValue: { production: false, apiBaseUrl: 'http://localhost:8080' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterView);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function fillValidForm(): void {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.email.set('john@example.com');
    component.password.set('Str0ng!Pass');
    component.confirmPassword.set('Str0ng!Pass');
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('success → auto-handoff', () => {
    it('should switch to verify-email after successful registration', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { id: 1, email: 'john@example.com', status: 'ACTIVE', verificationOtp: '123456' },
          { status: 201 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(sessionSpy.setSession).toHaveBeenCalledWith('pending', 'pending', {
        id: 1,
        email: 'john@example.com',
        emailVerified: false,
        roles: ['TRAVELER'],
      });
      expect(authModalSpy.setUserEmail).toHaveBeenCalledWith('john@example.com');
      expect(authModalSpy.switchView).toHaveBeenCalledWith('verify-email');
    });

    it('should store pending OTP in non-production when present', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { id: 1, email: 'john@example.com', verificationOtp: '654321' },
          { status: 201 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(authModalSpy.setPendingOtp).toHaveBeenCalledWith('654321');
    });

    it('should not store pending OTP when response has no OTP', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { id: 1, email: 'john@example.com' },
          { status: 201 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(authModalSpy.setPendingOtp).not.toHaveBeenCalled();
    });
  });

  describe('duplicate email error', () => {
    it('should render duplicate email error on 409 with EMAIL_ALREADY_IN_USE', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'EMAIL_ALREADY_IN_USE', message: 'email already in use' },
          { status: 409 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('An account with this email already exists');
      expect(authModalSpy.switchView).not.toHaveBeenCalled();
    });

    it('should show generic error for non-duplicate 409', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'SOME_OTHER_CONFLICT', message: 'something else' },
          { status: 409 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('something else');
    });
  });

  describe('other errors', () => {
    it('should show generic error for 400 validation', async () => {
      fetchSpy.mockResolvedValueOnce(
        jsonResponse(
          { code: 'VALIDATION_ERROR', message: 'invalid data' },
          { status: 400 },
        ),
      );
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('invalid data');
    });

    it('should show network error on fetch failure', async () => {
      fetchSpy.mockRejectedValueOnce(new Error('Network error'));
      fillValidForm();

      await component.onSubmit();

      expect(component.error()).toBe('Network error. Please try again.');
    });
  });

  describe('form validation prevents submission', () => {
    it('should not submit when form is invalid', async () => {
      component.firstName.set('');
      component.email.set('bad');
      component.password.set('short');

      await component.onSubmit();

      expect(fetchSpy).not.toHaveBeenCalled();
    });
  });
});
