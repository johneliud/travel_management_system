import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { HomePage } from './home';
import { SessionService } from '../../core/auth/session.service';
import { AuthModalService } from '../../core/auth/auth-modal.service';

function createSessionStub(overrides: { loggedIn?: boolean; roles?: string[] } = {}) {
  const loggedIn = signal(overrides.loggedIn ?? false);
  const roles = signal(overrides.roles ?? []);
  return {
    isLoggedIn: loggedIn.asReadonly(),
    roles: roles.asReadonly(),
    hasRole: (role: string) => roles().includes(role),
    setLoggedIn: (v: boolean) => loggedIn.set(v),
    setRoles: (r: string[]) => roles.set(r),
  };
}

function createAuthModalStub() {
  return {
    open: vi.fn(),
  };
}

describe('HomePage', () => {
  let sessionStub: ReturnType<typeof createSessionStub>;
  let authModalStub: ReturnType<typeof createAuthModalStub>;

  beforeEach(async () => {
    sessionStub = createSessionStub();
    authModalStub = createAuthModalStub();

    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: sessionStub },
        { provide: AuthModalService, useValue: authModalStub },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(HomePage);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render hero section', () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Travel, simplified.');
  });

  it('should render hero image via img tag', () => {
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const img = compiled.querySelector('img');
    expect(img).toBeTruthy();
    expect(img?.getAttribute('alt')).toBe('Tropical beach with palm trees and lounge chairs');
  });

  describe('logged-out', () => {
    beforeEach(() => {
      sessionStub.setLoggedIn(false);
    });

    it('should show CTA button that opens register modal', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const cta = compiled.querySelector('button');
      expect(cta?.textContent).toContain('Get started');
      cta?.click();
      expect(authModalStub.open).toHaveBeenCalledWith('register');
    });

    it('should show marketing section', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Why use Travel Management System?');
    });

    it('should show three value prop cards', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const cards = compiled.querySelectorAll('.grid > div');
      expect(cards.length).toBe(3);
    });

    it('should show sign-in link that opens login modal', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const signIn = Array.from(compiled.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Sign in'),
      );
      expect(signIn).toBeTruthy();
      signIn?.click();
      expect(authModalStub.open).toHaveBeenCalledWith('login');
    });

    it('should not show logged-in sections', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Discover trips');
      expect(compiled.textContent).not.toContain('Your travels');
      expect(compiled.textContent).not.toContain('Admin dashboard');
    });
  });

  describe('logged-in as TRAVELER', () => {
    beforeEach(() => {
      sessionStub.setLoggedIn(true);
      sessionStub.setRoles(['TRAVELER']);
    });

    it('should show traveler section', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Discover trips');
    });

    it('should show browse travels link', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const link = compiled.querySelector('a[href="/travels"]');
      expect(link).toBeTruthy();
    });

    it('should show hero CTA linking to travels', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const heroCta = compiled.querySelector('section a[href="/travels"]');
      expect(heroCta).toBeTruthy();
    });

    it('should not show marketing section', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Why use Travel Management System?');
    });

    it('should not show manager or admin sections', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Your travels');
      expect(compiled.textContent).not.toContain('Admin dashboard');
    });
  });

  describe('logged-in as TRAVEL_MANAGER', () => {
    beforeEach(() => {
      sessionStub.setLoggedIn(true);
      sessionStub.setRoles(['TRAVEL_MANAGER']);
    });

    it('should show manager section', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Your travels');
    });

    it('should show my travels and browse all links', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('a[href="/travels/mine"]')).toBeTruthy();
      expect(compiled.querySelector('a[href="/travels"]')).toBeTruthy();
    });

    it('should show hero CTA linking to my travels', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const heroCta = compiled.querySelector('section a[href="/travels/mine"]');
      expect(heroCta).toBeTruthy();
    });

    it('should not show traveler or admin sections', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Discover trips');
      expect(compiled.textContent).not.toContain('Admin dashboard');
    });
  });

  describe('logged-in as ADMIN', () => {
    beforeEach(() => {
      sessionStub.setLoggedIn(true);
      sessionStub.setRoles(['ADMIN']);
    });

    it('should show admin section', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Admin dashboard');
    });

    it('should show admin and browse links', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('a[href="/admin"]')).toBeTruthy();
      expect(compiled.querySelector('a[href="/travels"]')).toBeTruthy();
    });

    it('should show hero CTA linking to admin', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      const heroCta = compiled.querySelector('section a[href="/admin"]');
      expect(heroCta).toBeTruthy();
    });

    it('should not show traveler or manager sections', () => {
      const fixture = TestBed.createComponent(HomePage);
      fixture.detectChanges();
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Discover trips');
      expect(compiled.textContent).not.toContain('Your travels');
    });
  });
});
