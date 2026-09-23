import { Injectable, signal, computed, type Type } from '@angular/core';

export interface ModalConfig {
  component: Type<unknown>;
  data?: Record<string, unknown>;
  ariaLabel?: string;
  dismissible?: boolean;
}

export interface ModalRef {
  id: string;
  close: () => void;
}

interface ModalEntry {
  id: string;
  config: ModalConfig;
}

@Injectable({ providedIn: 'root' })
export class ModalService {
  private readonly _stack = signal<ModalEntry[]>([]);
  private previousFocus: HTMLElement | null = null;

  readonly stack = this._stack.asReadonly();
  readonly isOpen = computed(() => this._stack().length > 0);
  readonly activeConfig = computed<ModalConfig | null>(() => {
    const stack = this._stack();
    return stack.length > 0 ? stack[stack.length - 1].config : null;
  });

  open(config: ModalConfig): ModalRef {
    if (this._stack().length === 0) {
      this.previousFocus = document.activeElement as HTMLElement;
      document.body.style.overflow = 'hidden';
    }

    const id = crypto.randomUUID();
    const ref: ModalRef = {
      id,
      close: () => this.close(id),
    };

    this._stack.update((stack) => [...stack, { id, config }]);
    return ref;
  }

  close(id?: string): void {
    const stack = this._stack();
    if (stack.length === 0) return;

    if (id) {
      this._stack.update((s) => s.filter((entry) => entry.id !== id));
    } else {
      this._stack.update((s) => s.slice(0, -1));
    }

    if (this._stack().length === 0) {
      document.body.style.overflow = '';
      this.previousFocus?.focus();
      this.previousFocus = null;
    }
  }

  closeAll(): void {
    if (this._stack().length === 0) return;
    this._stack.set([]);
    document.body.style.overflow = '';
    this.previousFocus?.focus();
    this.previousFocus = null;
  }
}
