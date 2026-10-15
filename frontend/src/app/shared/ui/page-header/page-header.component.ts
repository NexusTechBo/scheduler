import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  imports: [],
  templateUrl: './page-header.component.html',
  host: { class: 'mb-5 sm:mb-7 flex shrink-0 flex-wrap items-center justify-between gap-4' },
})
export class PageHeaderComponent {
  title = input.required<string>();
  subtitle = input<string>();
}
