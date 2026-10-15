import { Component, input } from '@angular/core';
import { UiIconComponent, UiIconName } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-empty-state',
  imports: [UiIconComponent],
  template: `
    <div
      class="flex h-full flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed border-gray-900/10 px-6 py-10 text-center"
    >
      <span class="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/60 text-gray-400">
        <app-ui-icon [name]="icon()" class="h-6 w-6" />
      </span>
      <div>
        <p class="text-[15px] font-bold text-gray-700">{{ title() }}</p>
        @if (message()) {
          <p class="mt-1 text-sm font-medium text-gray-500">{{ message() }}</p>
        }
      </div>
      <ng-content />
    </div>
  `,
  host: { class: 'block' }
})
export class EmptyStateComponent {
  title = input.required<string>();
  message = input<string>();
  icon = input<UiIconName>('calendar');
}
