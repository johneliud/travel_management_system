import type { Routes } from '@angular/router';

export const routes: Routes = [
  // {
  //   path: '',
  //   loadComponent: () =>
  //     import('./pages/home/home').then(m => m.HomePage),
  // },
  {
    path: 'terms',
    loadComponent: () =>
      import('./pages/terms/terms').then(m => m.TermsPage),
  },
  {
    path: 'privacy',
    loadComponent: () =>
      import('./pages/privacy/privacy').then(m => m.PrivacyPage),
  },
];
