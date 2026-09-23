import { Component } from '@angular/core';

@Component({
  selector: 'app-terms',
  template: `
    <article class="prose prose-(--color-text) max-w-none space-y-6">
      <h1>Terms of Service</h1>

      <h2>1. Acceptance of Terms</h2>
      <p>
        By accessing and using the Travel Management System ("Service"), you agree to be bound by these Terms of Service.
        If you do not agree to these terms, please do not use the Service.
      </p>

      <h2>2. Use of Service</h2>
      <p>
        The Service is provided for travel planning and management purposes. You are responsible for maintaining
        the confidentiality of your account credentials and for all activities that occur under your account.
      </p>

      <h2>3. User Responsibilities</h2>
      <p>
        You agree to provide accurate information when creating an account and to keep your information up to date.
        You are responsible for safeguarding your password and for any activities or actions under your account.
      </p>

      <h2>4. Privacy</h2>
      <p>
        Your use of the Service is also governed by our
        <a routerLink="/privacy">Privacy Policy</a>, which is incorporated into these Terms by reference.
      </p>

      <h2>5. Modifications</h2>
      <p>
        We reserve the right to modify these Terms at any time. Changes will be effective immediately upon posting.
        Your continued use of the Service after any modifications indicates your acceptance of the updated Terms.
      </p>

      <h2>6. Contact</h2>
      <p>
        If you have questions about these Terms, please contact us through the application support channels.
      </p>
    </article>
  `,
})
export class TermsPage {}
