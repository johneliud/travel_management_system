import { InjectionToken } from '@angular/core';
import { environment } from '../../../environment/environment';

export interface Environment {
  production: boolean;
  apiBaseUrl: string;
}

export const ENVIRONMENT = new InjectionToken<Environment>('Application environment', {
  providedIn: 'root',
  factory: () => environment,
});
