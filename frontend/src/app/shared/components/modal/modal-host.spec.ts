import { TestBed, type ComponentFixture } from '@angular/core/testing';
import { Component, input } from '@angular/core';
import { ModalService } from '../../../core/modal/modal.service';
import { ModalHostComponent } from './modal-host';

@Component({
  template: `
    <button id="first-btn">First</button>
    <input id="test-input" type="text" />
    <button id="last-btn">Last</button>
  `,
})
class TestModalContent {
  readonly data = input<string>('');
}

@Component({
  template: '<p id="no-focus">No focusable elements</p>',
})
class TestModalContentNoFocusable {}

describe('ModalHostComponent', () => {
  let component: ModalHostComponent;
  let fixture: ComponentFixture<ModalHostComponent>;
  let modalService: ModalService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalHostComponent, TestModalContent, TestModalContentNoFocusable],
    }).compileComponents();

    fixture = TestBed.createComponent(ModalHostComponent);
    component = fixture.componentInstance;
    modalService = TestBed.inject(ModalService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should hide backdrop when no modal is open', () => {
    const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(backdrop).toBeTruthy();
    expect(backdrop.classList.contains('hidden')).toBeTruthy();
  });

  it('should show backdrop when modal is open', () => {
    modalService.open({ component: TestModalContent, ariaLabel: 'Test modal' });
    fixture.detectChanges();
    const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(backdrop).toBeTruthy();
    expect(backdrop.classList.contains('hidden')).toBeFalsy();
  });

  it('should set aria-label on the backdrop', () => {
    modalService.open({ component: TestModalContent, ariaLabel: 'My Dialog' });
    fixture.detectChanges();
    const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(backdrop.getAttribute('aria-label')).toBe('My Dialog');
  });

  it('should set aria-modal on the backdrop', () => {
    modalService.open({ component: TestModalContent });
    fixture.detectChanges();
    const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(backdrop.getAttribute('aria-modal')).toBe('true');
  });

  describe('backdrop click', () => {
    it('should close the modal when backdrop is clicked', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();
      const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
      backdrop.click();
      expect(modalService.isOpen()).toBeFalsy();
    });

    it('should not close when content area is clicked', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();
      const contentArea = fixture.nativeElement.querySelector('.bg-white');
      contentArea.click(new MouseEvent('click', { bubbles: true }));
      expect(modalService.isOpen()).toBeTruthy();
    });

    it('should not close on backdrop click when dismissible is false', () => {
      modalService.open({ component: TestModalContent, dismissible: false });
      fixture.detectChanges();
      const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
      backdrop.click();
      expect(modalService.isOpen()).toBeTruthy();
    });
  });

  describe('escape key', () => {
    it('should close the modal when Escape is pressed', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();
      const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
      backdrop.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(modalService.isOpen()).toBeFalsy();
    });

    it('should not close on Escape when dismissible is false', () => {
      modalService.open({ component: TestModalContent, dismissible: false });
      fixture.detectChanges();
      const backdrop = fixture.nativeElement.querySelector('[role="dialog"]');
      backdrop.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      expect(modalService.isOpen()).toBeTruthy();
    });
  });

  describe('dynamic rendering', () => {
    it('should render the component when modal is open', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();

      const container = fixture.nativeElement.querySelector('.bg-white');
      expect(container).toBeTruthy();
      expect(container.querySelector('#first-btn')).toBeTruthy();
      expect(container.querySelector('#test-input')).toBeTruthy();
      expect(container.querySelector('#last-btn')).toBeTruthy();
    });

    it('should remove rendered component when modal is closed', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#first-btn')).toBeTruthy();

      modalService.close();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#first-btn')).toBeFalsy();
    });

    it('should replace component when switching modals', () => {
      modalService.open({ component: TestModalContent });
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#first-btn')).toBeTruthy();

      modalService.close();
      modalService.open({ component: TestModalContentNoFocusable });
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('#first-btn')).toBeFalsy();
      expect(fixture.nativeElement.querySelector('#no-focus')).toBeTruthy();
    });
  });
});
