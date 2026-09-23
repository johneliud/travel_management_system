import { Injectable, inject } from '@angular/core';
import { SessionService } from './session.service';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  emailVerified: boolean;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = inject(SessionService);

  async login(email: string, password: string): Promise<void> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-API-Version': '1' },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('Invalid email or password');
      }
      const body = await response.json().catch(() => ({}));
      throw new Error(body.message || 'Login failed');
    }

    const data: LoginResponse = await response.json();
    this.session.setSession(data.accessToken, data.refreshToken, {
      id: data.userId,
      email: data.email,
      emailVerified: data.emailVerified ?? false,
      roles: data.roles ?? [],
    });
  }
}
