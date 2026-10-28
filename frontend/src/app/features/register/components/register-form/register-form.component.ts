import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  USERNAME_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
  usernameValidator,
} from '../../../../shared/utils/validation-messages.util';
import { RegisterService } from '../../register.service';

@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent],
  templateUrl: './register-form.component.html',
  host: { class: 'flex min-h-screen items-center justify-center px-4 py-10 sm:px-6' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterFormComponent {
  protected readonly register = inject(RegisterService);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', notBlank],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9]{3,50}$/)]],
    phoneNumber: ['', [notBlank, phoneValidator]],
    adminName: ['', notBlank],
    adminCi: ['', [notBlank, ciValidator]],
    adminUsername: ['', [Validators.required, usernameValidator]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly suffix = toSignal(
    this.form.controls.slug.valueChanges.pipe(
      map((value) => (this.form.controls.slug.valid ? value : '')),
    ),
    { initialValue: '' },
  );
  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly slugErrors = {
    pattern: 'De 3 a 50 caracteres: solo minúsculas y números (sin "@" ni ".com")',
  };
  protected readonly usernameErrors = USERNAME_ERRORS;

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.register.register(this.form.getRawValue());
  }
}
