import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ResendActivationComponent } from '../../../../shared/clinic-activation/components/resend-activation/resend-activation.component';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { LoginService } from '../../login.service';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent, ResendActivationComponent],
  templateUrl: './login-form.component.html',
  host: { class: 'flex min-h-screen items-center justify-center px-4 py-10 sm:px-6' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  private readonly loginService = inject(LoginService);
  protected readonly submitting = this.loginService.submitting;
  protected readonly pendingActivation = this.loginService.pendingActivation;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: [usernameFromState(inject(Location).getState()), Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, password } = this.form.getRawValue();
    this.loginService.login(username.trim(), password);
  }
}

/** The activation page passes the admin username through the navigation state. */
function usernameFromState(state: unknown): string {
  if (state && typeof state === 'object' && 'username' in state) {
    return typeof state.username === 'string' ? state.username : '';
  }
  return '';
}
