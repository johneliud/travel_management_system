import { TestBed } from '@angular/core/testing';
import { AuthModalService } from './auth-modal.service';
import { ModalService } from '../modal/modal.service';

describe('AuthModalService', () => {
  let service: AuthModalService;
  let modalService: ModalService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthModalService);
    modalService = TestBed.inject(ModalService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('open', () => {
    it('should open modal with default login view', () => {
      service.open();
      expect(service.currentView()).toBe('login');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should open modal with specified view', () => {
      service.open('register');
      expect(service.currentView()).toBe('register');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should open modal with verify-email view', () => {
      service.open('verify-email');
      expect(service.currentView()).toBe('verify-email');
    });
  });

  describe('switchView', () => {
    it('should switch view without closing modal', () => {
      service.open('login');
      service.switchView('register');
      expect(service.currentView()).toBe('register');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should switch from register to verify-email', () => {
      service.open('register');
      service.switchView('verify-email');
      expect(service.currentView()).toBe('verify-email');
    });

    it('should switch from login to forgot-password', () => {
      service.open('login');
      service.switchView('forgot-password');
      expect(service.currentView()).toBe('forgot-password');
    });

    it('should switch from forgot-password to reset-password', () => {
      service.open('forgot-password');
      service.switchView('reset-password');
      expect(service.currentView()).toBe('reset-password');
    });

    it('should switch to change-password from any view', () => {
      service.open('login');
      service.switchView('change-password');
      expect(service.currentView()).toBe('change-password');
    });
  });

  describe('close', () => {
    it('should close the modal', () => {
      service.open('login');
      service.close();
      expect(modalService.isOpen()).toBeFalsy();
    });
  });

  describe('setUserEmail', () => {
    it('should set user email', () => {
      service.setUserEmail('test@example.com');
      expect(service.userEmail()).toBe('test@example.com');
    });

    it('should update user email', () => {
      service.setUserEmail('first@example.com');
      service.setUserEmail('second@example.com');
      expect(service.userEmail()).toBe('second@example.com');
    });
  });

  describe('viewTitle', () => {
    it('should return correct title for login', () => {
      service.open('login');
      expect(service.viewTitle()).toBe('Welcome back');
    });

    it('should return correct title for register', () => {
      service.open('register');
      expect(service.viewTitle()).toBe('Create your account');
    });

    it('should return correct title for verify-email', () => {
      service.open('verify-email');
      expect(service.viewTitle()).toBe('Verify your email');
    });

    it('should return correct title for forgot-password', () => {
      service.open('forgot-password');
      expect(service.viewTitle()).toBe('Reset your password');
    });

    it('should return correct title for reset-password', () => {
      service.open('reset-password');
      expect(service.viewTitle()).toBe('Set new password');
    });

    it('should return correct title for change-password', () => {
      service.open('change-password');
      expect(service.viewTitle()).toBe('Change password');
    });

    it('should update title when view switches', () => {
      service.open('login');
      expect(service.viewTitle()).toBe('Welcome back');
      service.switchView('register');
      expect(service.viewTitle()).toBe('Create your account');
    });
  });

  describe('viewSubtitle', () => {
    it('should return correct subtitle for login', () => {
      service.open('login');
      expect(service.viewSubtitle()).toBe('Sign in to access your travel dashboard');
    });

    it('should return correct subtitle for register', () => {
      service.open('register');
      expect(service.viewSubtitle()).toBe('Start planning your next adventure');
    });

    it('should return correct subtitle for verify-email', () => {
      service.open('verify-email');
      expect(service.viewSubtitle()).toBe('Enter the 6-digit code sent to your email');
    });

    it('should update subtitle when view switches', () => {
      service.open('login');
      expect(service.viewSubtitle()).toBe('Sign in to access your travel dashboard');
      service.switchView('register');
      expect(service.viewSubtitle()).toBe('Start planning your next adventure');
    });
  });

  describe('view transitions', () => {
    it('should support login → register flow', () => {
      service.open('login');
      service.switchView('register');
      expect(service.currentView()).toBe('register');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should support register → login flow', () => {
      service.open('register');
      service.switchView('login');
      expect(service.currentView()).toBe('login');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should support register → verify-email auto-handoff', () => {
      service.open('register');
      service.switchView('verify-email');
      expect(service.currentView()).toBe('verify-email');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should support login → forgot-password → reset-password flow', () => {
      service.open('login');
      service.switchView('forgot-password');
      expect(service.currentView()).toBe('forgot-password');
      service.switchView('reset-password');
      expect(service.currentView()).toBe('reset-password');
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should support any → change-password', () => {
      service.open('login');
      service.switchView('change-password');
      expect(service.currentView()).toBe('change-password');
    });
  });
});
