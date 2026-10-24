import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  input,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { ResendActivationComponent } from '../../../../shared/clinic-activation/components/resend-activation/resend-activation.component';
import { UiIconComponent } from '../../../../shared/ui/ui-icon/ui-icon.component';

@Component({
  selector: 'app-register-success',
  imports: [RouterLink, ResendActivationComponent, UiIconComponent],
  templateUrl: './register-success.component.html',
  host: { class: 'flex min-h-screen items-center justify-center px-4 py-10 sm:px-6' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterSuccessComponent {
  readonly email = input.required<string>();

  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');

  constructor() {
    afterNextRender(() => this.heading().nativeElement.focus());
  }
}
