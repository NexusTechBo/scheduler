import { Component, input } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { IconPipe } from '../../pipes/icon.pipe';
import { IconName } from '../../../core/icons/icon-registry';

@Component({
  selector: 'app-icon',
  imports: [AsyncPipe, IconPipe],
  template: `<span class="block h-full w-full" [innerHTML]="name() | icon | async"></span>`,
  host: { class: 'inline-block' }
})
export class IconComponent {
  name = input.required<IconName>();
}
