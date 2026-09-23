import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TermsPage } from './terms';

describe('TermsPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TermsPage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(TermsPage);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the heading', () => {
    const fixture = TestBed.createComponent(TermsPage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Terms of Service');
  });

  it('should have sections', () => {
    const fixture = TestBed.createComponent(TermsPage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const headings = compiled.querySelectorAll('h2');
    expect(headings.length).toBeGreaterThanOrEqual(1);
  });
});
