import { ChangeDetectionStrategy, Component, booleanAttribute, input, output } from '@angular/core';
import { AlertComponent } from '../alert/alert.component';

@Component({
  selector: 'app-load-error',
  imports: [AlertComponent],
  templateUrl: './load-error.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadErrorComponent {
  message = input.required<string>();
  retryLabel = input('Reintentar');
  retryDisabled = input(false, { transform: booleanAttribute });
  retry = output<void>();

  protected onRetry(): void {
    if (!this.retryDisabled()) this.retry.emit();
  }
}
