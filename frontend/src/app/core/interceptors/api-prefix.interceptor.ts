import type { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { ENVIRONMENT } from '../services/environment.token';

export const apiPrefixInterceptor: HttpInterceptorFn = (req, next) => {
  const env = inject(ENVIRONMENT);

  if (req.url.startsWith(env.apiBaseUrl)) {
    return next(req);
  }

  const cloned = req.clone({
    url: `${env.apiBaseUrl}${req.url}`,
    setHeaders: { 'X-API-Version': '1' },
  });

  return next(cloned);
};
