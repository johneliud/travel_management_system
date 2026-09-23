import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacy',
  imports: [RouterLink],
  template: `
    <article class="prose prose-(--color-text) max-w-none space-y-6">
      <h1>Privacy Policy</h1>

      <h2>1. Information We Collect</h2>
      <p>
        We collect information you provide directly, such as your name, email address, and travel preferences.
        We also collect usage data automatically as you interact with the Service.
      </p>

      <h2>2. How We Use Your Information</h2>
      <p>
        We use your information to provide and improve the Service, to communicate with you, and to ensure
        the security of your account. We do not sell your personal information to third parties.
      </p>

      <h2>3. Data Storage</h2>
      <p>
        Your data is stored securely using industry-standard encryption and security practices.
        We retain your information for as long as your account is active or as needed to provide the Service.
      </p>

      <h2>4. Your Rights</h2>
      <p>
        You have the right to access, update, or delete your personal information at any time through
        your account settings. You may also contact us to request a copy of all data we hold about you.
      </p>

      <h2>5. Cookies</h2>
      <p>
        The Service uses essential cookies to maintain your session and authentication state.
        We do not use tracking or advertising cookies.
      </p>

      <h2>6. Changes to This Policy</h2>
      <p>
        We may update this Privacy Policy from time to time. We will notify you of any material changes
        by posting the new policy on this page with an updated effective date.
      </p>

      <h2>7. Contact</h2>
      <p>
        If you have questions about this Privacy Policy, please contact us through the application support channels.
      </p>

      <p class="text-sm text-(--color-text-muted)">
        See also our <a routerLink="/terms">Terms of Service</a>.
      </p>
    </article>
  `,
})
export class PrivacyPage {}
