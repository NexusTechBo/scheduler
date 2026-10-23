import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  booleanAttribute,
  computed,
  inject,
  input,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormFieldComponent } from '../../../ui/forms/form-field/form-field.component';
import { ResendActivationService } from './resend-activation.service';

@Component({
  selector: 'app-resend-activation',
  imports: [ReactiveFormsModule, FormFieldComponent],
  providers: [ResendActivationService],
  templateUrl: './resend-activation.component.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResendActivationComponent implements OnInit {
  readonly email = input<string>();
  readonly label = input('Reenviar correo');
  readonly fieldId = input('resend-activation-email');
  readonly startCooldown = input(false, { transform: booleanAttribute });

  protected readonly resend = inject(ResendActivationService);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly buttonLabel = computed(() => {
    if (this.resend.sending()) return 'Enviando…';
    const remaining = this.resend.remaining();
    return remaining > 0 ? `Reenviar en ${remaining} s` : this.label();
  });

  ngOnInit(): void {
    if (this.startCooldown()) this.resend.startCooldown();
  }

  protected submit(): void {
    const fixedEmail = this.email();
    if (fixedEmail) {
      this.resend.resend(fixedEmail, false);
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.resend.resend(this.form.controls.email.value.trim());
  }
}
