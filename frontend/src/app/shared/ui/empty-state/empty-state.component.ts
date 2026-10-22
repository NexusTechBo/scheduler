import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { UiIconComponent, UiIconName } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-empty-state',
  imports: [UiIconComponent],
  templateUrl: './empty-state.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  title = input.required<string>();
  message = input<string>();
  icon = input<UiIconName>('calendar');
}
