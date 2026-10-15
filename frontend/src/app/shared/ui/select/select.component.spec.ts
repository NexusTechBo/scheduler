import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { SelectComponent, SelectOption } from './select.component';

@Component({
  imports: [ReactiveFormsModule, SelectComponent],
  template: `<app-select id="fruit" [formControl]="control" [options]="options" />`,
})
class HostComponent {
  control = new FormControl<string | null>('b');
  options: SelectOption<string>[] = [
    { value: 'a', label: 'Apple' },
    { value: 'b', label: 'Banana' },
    { value: 'c', label: 'Cherry', disabled: true },
    { value: 'd', label: 'Date' },
  ];
}

describe('SelectComponent', () => {
  function setup() {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const trigger: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    const press = (key: string) => {
      trigger.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
      fixture.detectChanges();
    };
    return { fixture, trigger, press };
  }

  it('puts the id on the trigger, not the host', () => {
    const { fixture, trigger } = setup();
    expect(trigger.id).toBe('fruit');
    expect(fixture.nativeElement.querySelector('app-select').hasAttribute('id')).toBeFalse();
    expect(trigger.textContent).toContain('Banana');
  });

  it('opens on ArrowDown, skips disabled options and selects with Enter', () => {
    const { fixture, trigger, press } = setup();
    press('ArrowDown');
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    const active = () => document.getElementById(trigger.getAttribute('aria-activedescendant')!);
    expect(active()?.textContent).toContain('Banana');

    press('ArrowDown');
    expect(active()?.textContent).toContain('Date');
    press('Enter');
    expect(fixture.componentInstance.control.value).toBe('d');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(fixture.componentInstance.control.touched).toBeTrue();
  });

  it('jumps by first letter, Home/End and Esc closes without changing the value', () => {
    const { fixture, trigger, press } = setup();
    press('a');
    const active = () => document.getElementById(trigger.getAttribute('aria-activedescendant')!);
    expect(active()?.textContent).toContain('Apple');
    press('End');
    expect(active()?.textContent).toContain('Date');
    press('Home');
    expect(active()?.textContent).toContain('Apple');
    press('Escape');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(fixture.componentInstance.control.value).toBe('b');
  });

  it('respects the disabled state of the form control', () => {
    const { fixture, trigger, press } = setup();
    fixture.componentInstance.control.disable();
    fixture.detectChanges();
    expect(trigger.disabled).toBeTrue();
    press('ArrowDown');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
  });
});
