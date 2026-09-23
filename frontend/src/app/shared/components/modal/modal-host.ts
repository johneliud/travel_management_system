import {
  Component,
  inject,
  effect,
  viewChild,
  type ElementRef,
  createComponent,
  EnvironmentInjector,
  ApplicationRef,
} from '@angular/core';
import { ModalService } from '../../../core/modal/modal.service';

@Component({
  selector: 'app-modal-host',
  template: `
    <div
      class="fixed inset-0 z-(--z-modal) bg-black/50 flex items-center justify-center p-4"
      [class]="isOpen() ? '' : 'hidden'"
      (click)="onBackdropClick($event)"
      (keydown)="onKeyDown($event)"
      role="dialog"
      aria-modal="true"
      [attr.aria-label]="activeConfig()?.ariaLabel"
    >
      <div
        class="bg-white rounded-xl shadow-2xl max-w-3xl! w-full overflow-hidden animate-slide-in max-h-[90vh] flex flex-col"
        (click)="$event.stopPropagation()"
        tabindex="-1"
        #modalContainer
      ></div>
    </div>
  `,
})
export class ModalHostComponent {
  private readonly modalService = inject(ModalService);
  private readonly envInjector = inject(EnvironmentInjector);
  private readonly appRef = inject(ApplicationRef);
  private readonly containerRef = viewChild<ElementRef<HTMLDivElement>>('modalContainer');

  readonly isOpen = this.modalService.isOpen;
  readonly activeConfig = this.modalService.activeConfig;

  private currentHostElement: HTMLElement | null = null;
  private currentComponentRef: ReturnType<typeof createComponent> | null = null;

  constructor() {
    effect(() => {
      const ref = this.containerRef();
      const config = this.activeConfig();
      if (ref) {
        this.renderContent(config, ref.nativeElement);
      }
    });
  }

  private renderContent(
    config: ReturnType<ModalService['activeConfig']>,
    container: HTMLElement,
  ): void {
    if (this.currentHostElement) {
      if (this.currentComponentRef) {
        this.appRef.detachView(this.currentComponentRef.hostView);
        this.currentComponentRef.destroy();
        this.currentComponentRef = null;
      }
      this.currentHostElement.remove();
      this.currentHostElement = null;
    }

    if (config) {
      const componentRef = createComponent(config.component, {
        environmentInjector: this.envInjector,
      });

      if (config.data) {
        Object.entries(config.data).forEach(([key, value]) => {
          if (value !== undefined) {
            componentRef.setInput(key, value);
          }
        });
      }

      this.appRef.attachView(componentRef.hostView);
      container.appendChild(componentRef.location.nativeElement);
      this.currentHostElement = componentRef.location.nativeElement;
      this.currentComponentRef = componentRef;
    }
  }

  onBackdropClick(_event: MouseEvent): void {
    if (this.activeConfig()?.dismissible !== false) {
      this.modalService.close();
    }
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      if (this.activeConfig()?.dismissible !== false) {
        this.modalService.close();
      }
    } else if (event.key === 'Tab') {
      this.trapFocus(event);
    }
  }

  private trapFocus(event: KeyboardEvent): void {
    const container = this.containerRef()?.nativeElement;
    if (!container) return;

    const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
    ) as NodeListOf<HTMLElement>;

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey) {
      if (document.activeElement === firstElement) {
        event.preventDefault();
        lastElement.focus();
      }
    } else {
      if (document.activeElement === lastElement) {
        event.preventDefault();
        firstElement.focus();
      }
    }
  }
}
