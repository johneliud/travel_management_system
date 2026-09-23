import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { ModalService } from './modal.service';

@Component({ template: 'test content' })
class TestModalContent {}

@Component({ template: 'test content 2' })
class TestModalContent2 {}

describe('ModalService', () => {
  let service: ModalService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ModalService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('open', () => {
    it('should add a modal to the stack', () => {
      service.open({ component: TestModalContent });
      expect(service.stack().length).toBe(1);
    });

    it('should set isOpen to true', () => {
      expect(service.isOpen()).toBeFalsy();
      service.open({ component: TestModalContent });
      expect(service.isOpen()).toBeTruthy();
    });

    it('should set activeConfig to the opened config', () => {
      const config = { component: TestModalContent, ariaLabel: 'Test' };
      service.open(config);
      expect(service.activeConfig()).toEqual(config);
    });

    it('should return a ModalRef with id and close method', () => {
      const ref = service.open({ component: TestModalContent });
      expect(ref.id).toBeTruthy();
      expect(typeof ref.close).toBe('function');
    });

    it('should lock body scroll', () => {
      service.open({ component: TestModalContent });
      expect(document.body.style.overflow).toBe('hidden');
    });

    it('should support stacking multiple modals', () => {
      service.open({ component: TestModalContent });
      service.open({ component: TestModalContent2 });
      expect(service.stack().length).toBe(2);
      expect(service.activeConfig()?.component).toBe(TestModalContent2);
    });
  });

  describe('close', () => {
    it('should remove the topmost modal', () => {
      service.open({ component: TestModalContent });
      service.open({ component: TestModalContent2 });
      service.close();
      expect(service.stack().length).toBe(1);
      expect(service.activeConfig()?.component).toBe(TestModalContent);
    });

    it('should unlock body scroll when all modals are closed', () => {
      service.open({ component: TestModalContent });
      service.close();
      expect(service.isOpen()).toBeFalsy();
      expect(document.body.style.overflow).toBe('');
    });

    it('should close a specific modal by id', () => {
      const ref1 = service.open({ component: TestModalContent });
      service.open({ component: TestModalContent2 });
      service.close(ref1.id);
      expect(service.stack().length).toBe(1);
      expect(service.activeConfig()?.component).toBe(TestModalContent2);
    });

    it('should do nothing when stack is empty', () => {
      service.close();
      expect(service.isOpen()).toBeFalsy();
    });
  });

  describe('closeAll', () => {
    it('should clear the entire stack', () => {
      service.open({ component: TestModalContent });
      service.open({ component: TestModalContent2 });
      service.closeAll();
      expect(service.stack().length).toBe(0);
      expect(service.isOpen()).toBeFalsy();
      expect(service.activeConfig()).toBeNull();
    });

    it('should unlock body scroll', () => {
      service.open({ component: TestModalContent });
      service.closeAll();
      expect(document.body.style.overflow).toBe('');
    });

    it('should do nothing when stack is empty', () => {
      service.closeAll();
      expect(service.isOpen()).toBeFalsy();
    });
  });

  describe('focus restoration', () => {
    it('should restore focus to the previously focused element', () => {
      const button = document.createElement('button');
      document.body.appendChild(button);
      button.focus();

      service.open({ component: TestModalContent });
      service.close();

      expect(document.activeElement).toBe(button);
      document.body.removeChild(button);
    });
  });
});
