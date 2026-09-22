import { Component, signal, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { UpperCasePipe } from '@angular/common';
import { SessionService } from '../../../core/auth/session.service';
import { ModalService } from '../../../core/modal/modal.service';
import { NAV_ITEMS } from '../../../core/nav.config';
import { IconComponent } from '../icon/icon';
import { AuthModal } from './auth-modal';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, UpperCasePipe, IconComponent],
  templateUrl: './header.html',
  host: {
    '(document:click)': 'onDocumentClick($event)',
    class: 'block',
  },
})
export class Header {
  private readonly session = inject(SessionService);
  private readonly modalService = inject(ModalService);

  readonly isLoggedIn = this.session.isLoggedIn;
  readonly emailVerified = this.session.emailVerified;
  readonly user = this.session.user;
  readonly roles = this.session.roles;

  readonly mobileMenuOpen = signal(false);
  readonly userMenuOpen = signal(false);

  readonly visibleNavItems = computed(() => {
    const userRoles = this.roles();
    return NAV_ITEMS.filter((item) => !item.roles || item.roles.some((r) => userRoles.includes(r)));
  });

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((v) => !v);

    if (this.mobileMenuOpen()) {
      this.userMenuOpen.set(false);
    }
  }

  toggleUserMenu(): void {
    this.userMenuOpen.update((v) => !v);
  }

  closeUserMenu(): void {
    this.userMenuOpen.set(false);
  }

  openLogin(): void {
    this.modalService.open({ component: AuthModal, data: { view: 'login' }, ariaLabel: 'Log in' });
    this.mobileMenuOpen.set(false);
  }

  openRegister(): void {
    this.modalService.open({ component: AuthModal, data: { view: 'register' }, ariaLabel: 'Register' });
    this.mobileMenuOpen.set(false);
  }

  openVerifyEmail(): void {
    this.modalService.open({ component: AuthModal, data: { view: 'verify-email' }, ariaLabel: 'Verify email' });
    this.closeUserMenu();
    this.mobileMenuOpen.set(false);
  }

  logout(): void {
    this.session.clearSession();
    this.closeUserMenu();
    this.mobileMenuOpen.set(false);
  }

  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    if (!target.closest('[data-user-menu]')) {
      this.userMenuOpen.set(false);
    }
  }
}
