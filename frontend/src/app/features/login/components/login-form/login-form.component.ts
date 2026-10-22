import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import { LoginService } from '../../services/login.service';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent],
  templateUrl: './login-form.component.html',
  host: { class: 'flex min-h-screen items-center justify-center px-4 py-10 sm:px-6' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  private readonly loginService = inject(LoginService);
  protected readonly submitting = this.loginService.submitting;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', Validators.required],
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
