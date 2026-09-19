import { inject } from '@angular/core';
import { ENVIRONMENT } from './environment.token';

export function getApiUrl(): string {
  return inject(ENVIRONMENT).apiBaseUrl;
}
