import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PrivacyPage } from './privacy';

describe('PrivacyPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivacyPage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(PrivacyPage);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the heading', () => {
    const fixture = TestBed.createComponent(PrivacyPage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Privacy Policy');
  });

  it('should have sections', () => {
    const fixture = TestBed.createComponent(PrivacyPage);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const headings = compiled.querySelectorAll('h2');
    expect(headings.length).toBeGreaterThanOrEqual(1);
  });
});
