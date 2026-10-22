import { DialogConfig, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

let nextId = 0;

export function labelledByFrame(): Pick<DialogConfig, 'id' | 'ariaLabelledBy'> {
  const id = `app-dialog-${nextId++}`;
  return { id, ariaLabelledBy: `${id}-title` };
}

@Component({
  selector: 'app-dialog-frame',
  imports: [UiIconComponent],
  templateUrl: './dialog-frame.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DialogFrameComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();

  readonly closed = output<void>();

  protected readonly titleId = `${inject(DialogRef).id}-title`;
}
