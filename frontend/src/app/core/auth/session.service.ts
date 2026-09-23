import { Injectable, signal, computed } from '@angular/core';

export interface User {
  id: number;
  email: string;
  emailVerified: boolean;
  roles: string[];
}

export type AuthModalView = 'login' | 'register' | 'verify-email' | 'forgot-password' | 'reset-password' | 'change-password';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly accessToken = signal<string | null>(null);
  private readonly refreshToken = signal<string | null>(null);
  private readonly currentUser = signal<User | null>(null);

  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly user = computed(() => this.currentUser());
  readonly emailVerified = computed(() => this.currentUser()?.emailVerified ?? false);
  readonly roles = computed(() => this.currentUser()?.roles ?? []);
  readonly token = computed(() => this.accessToken());

  constructor() {
    this.restoreFromStorage();
  }

  setSession(accessToken: string, refreshToken: string, user: User): void {
    this.accessToken.set(accessToken);
    this.refreshToken.set(refreshToken);
    this.currentUser.set(user);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));
    }
  }

  clearSession(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    this.currentUser.set(null);
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user');
    }
  }

  updateUser(partial: Partial<User>): void {
    const current = this.currentUser();
    if (current) {
      const updated = { ...current, ...partial };
      this.currentUser.set(updated);
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('user', JSON.stringify(updated));
      }
    }
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  private restoreFromStorage(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      const accessToken = localStorage.getItem('access_token');
      const refreshToken = localStorage.getItem('refresh_token');
      const userJson = localStorage.getItem('user');
      if (accessToken && refreshToken && userJson) {
        this.accessToken.set(accessToken);
        this.refreshToken.set(refreshToken);
        this.currentUser.set(JSON.parse(userJson));
      }
    } catch {
      this.clearSession();
    }
  }
}
