import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  template: `
    <footer class="bg-(--color-surface) border-t border-(--color-border)">
      <div class="max-w-screen-2xl mx-auto px-4 sm:px-6 py-8">
        <div class="flex flex-row items-start justify-between gap-8">
          <!-- Brand -->
          <div class="lg:max-w-xs">
            <a routerLink="/" class="text-xl font-semibold text-(--color-text) font-(family-name:--font-heading)">
              TMS
            </a>
            <p class="mt-2 text-sm text-(--color-text-muted)">
              Plan, manage, and track your travels with ease.
            </p>
          </div>

          <!-- Links -->
          <div class="flex flex-wrap gap-8 lg:gap-12">
            <!-- Navigation -->
            <div>
              <h4 class="text-sm font-semibold text-(--color-text) mb-3">Navigation</h4>
              <ul class="space-y-2">
                <li>
                  <a routerLink="/travels" class="text-sm text-(--color-text-muted) hover:text-(--color-primary) transition-colors">
                    Browse Travels
                  </a>
                </li>
              </ul>
            </div>

            <!-- Legal -->
            <div>
              <h4 class="text-sm font-semibold text-(--color-text) mb-3">Legal</h4>
              <ul class="space-y-2">
                <li>
                  <a routerLink="/privacy" class="text-sm text-(--color-text-muted) hover:text-(--color-primary) transition-colors">
                    Privacy Policy
                  </a>
                </li>
                <li>
                  <a routerLink="/terms" class="text-sm text-(--color-text-muted) hover:text-(--color-primary) transition-colors">
                    Terms of Service
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Bottom bar -->
        <div class="mt-8 pt-6 border-t border-(--color-border) flex flex-col sm:flex-row items-center justify-between gap-4">
          <p class="text-xs text-(--color-text-muted)">
            &copy; {{ currentYear }} Travel Management System
          </p>
        </div>
      </div>
    </footer>
  `,
})
export class Footer {
  readonly currentYear = new Date().getFullYear();
}
