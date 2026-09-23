import type { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { SessionService } from '../auth/session.service';
import { AuthModalService } from '../auth/auth-modal.service';

export const authGuard: CanActivateFn = (route) => {
  const session = inject(SessionService);
  const authModal = inject(AuthModalService);
  const router = inject(Router);

  if (session.isLoggedIn()) {
    return true;
  }

  const returnUrl = route.url.map((segment) => segment.path).join('/') || '/';
  authModal.open('login', { redirectUrl: returnUrl, dismissible: true });
  return router.createUrlTree(['/']);
};
