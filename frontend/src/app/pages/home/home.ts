import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { SessionService } from '../../core/auth/session.service';
import { AuthModalService } from '../../core/auth/auth-modal.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink, NgOptimizedImage],
  host: { class: 'block' },
  templateUrl: './home.html',
})
export class HomePage {
  private readonly session = inject(SessionService);
  private readonly authModal = inject(AuthModalService);

  readonly isLoggedIn = this.session.isLoggedIn;
  readonly hasRole = (role: string) => this.session.hasRole(role);

  readonly homeLink = computed(() => {
    if (this.session.hasRole('ADMIN')) return '/admin';
    if (this.session.hasRole('TRAVEL_MANAGER')) return '/travels/mine';
    return '/travels';
  });

  readonly homeLinkLabel = computed(() => {
    if (this.session.hasRole('ADMIN')) return 'Admin dashboard';
    if (this.session.hasRole('TRAVEL_MANAGER')) return 'My travels';
    return 'Browse travels';
  });

  openLogin(): void {
    this.authModal.open('login');
  }

  openRegister(): void {
    this.authModal.open('register');
  }
}
