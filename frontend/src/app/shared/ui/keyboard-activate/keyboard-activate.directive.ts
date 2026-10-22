import { Directive } from '@angular/core';

@Directive({
  selector: '[appKeyboardActivate]',
  host: {
    role: 'button',
    tabindex: '0',
    '(keydown.enter)': 'activate($event)',
    '(keydown.space)': 'activate($event)',
  },
})
export class KeyboardActivateDirective {
  protected activate(event: Event): void {
    event.preventDefault();
    (event.currentTarget as HTMLElement).click();
  }
}
