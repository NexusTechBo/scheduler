import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormFieldComponent } from '../../../../shared/ui/forms/form-field/form-field.component';
import {
  CI_ERRORS,
  PHONE_ERRORS,
  ciValidator,
  notBlank,
  phoneValidator,
} from '../../../../shared/utils/validation-messages.util';
import { RegisterService } from '../../services/register.service';

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
    subdomain: ['', [Validators.required, Validators.pattern(/^[a-z0-9]{3,50}$/)]],
    phoneNumber: ['', [notBlank, phoneValidator]],
    adminName: ['', notBlank],
    adminCi: ['', [notBlank, ciValidator]],
    adminUsername: ['', [Validators.required, Validators.pattern(/^[a-z0-9._-]{3,50}$/)]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly suffix = toSignal(
    this.form.controls.subdomain.valueChanges.pipe(
      map((value) => (this.form.controls.subdomain.valid ? value : '')),
    ),
    { initialValue: '' },
  );
  protected readonly ciErrors = CI_ERRORS;
  protected readonly phoneErrors = PHONE_ERRORS;
  protected readonly subdomainErrors = {
    pattern: 'De 3 a 50 caracteres: solo minúsculas y números (sin "@" ni ".com")',
  };
  protected readonly usernameErrors = {
    pattern: 'De 3 a 50 caracteres: minúsculas, números, punto, guion o guion bajo',
  };

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.register.register(this.form.getRawValue());
  }
}
