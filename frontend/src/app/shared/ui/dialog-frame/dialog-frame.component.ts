import { CdkDialogContainer, DialogRef } from '@angular/cdk/dialog';
import { Component, inject, input, output } from '@angular/core';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

/**
 * Glass surface for every CDK dialog: title, optional subtitle, close button, body (default slot)
 * and a footer slot (`<div dialogFooter>`). Open dialogs with `Dialog.open(..., { backdropClass: 'glass-backdrop' })`.
 */
let nextId = 0;

@Component({
  selector: 'app-dialog-frame',
  imports: [UiIconComponent],
  template: `
    <div
      class="glass-modal relative flex max-h-[90vh] flex-col rounded-3xl p-6"
      [class]="widthClass()"
    >
      <div class="mb-5 flex shrink-0 items-start justify-between gap-3 pr-10">
        <div>
          <h2 [id]="titleId" class="text-xl font-extrabold text-gray-900 outline-none">{{ title() }}</h2>
          @if (subtitle()) {
            <p class="text-sm font-medium text-gray-500">{{ subtitle() }}</p>
          }
        </div>
      </div>

      <div class="-mx-1 min-h-0 flex-1 overflow-y-auto px-1">
        <ng-content />
      </div>

      <div class="mt-6 flex shrink-0 flex-wrap justify-end gap-2.5 empty:hidden">
        <ng-content select="[dialogFooter]" />
      </div>

      <!-- Last in DOM so CDK's default autoFocus lands on the first field/action, not on "Cerrar". -->
      <button
        type="button"
        class="btn-icon absolute right-5 top-5"
        (click)="closed.emit()"
        aria-label="Cerrar"
      >
        <app-ui-icon name="x" class="h-[18px] w-[18px]" />
      </button>
    </div>
  `,
})
export class DialogFrameComponent {
  title = input.required<string>();
  subtitle = input<string>();
  widthClass = input('w-[28rem] max-w-[calc(100vw-2rem)]');

  closed = output<void>();

  protected readonly titleId = `dialog-title-${nextId++}`;

  constructor() {
    // Name the CDK dialog (role="dialog") after this title; same hook MatDialogTitle uses.
    const container = inject(DialogRef, { optional: true })?.containerInstance;
    if (container instanceof CdkDialogContainer) {
      container._addAriaLabelledBy(this.titleId);
    }
  }
}
