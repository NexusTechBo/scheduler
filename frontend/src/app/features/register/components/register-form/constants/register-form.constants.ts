import { Validators } from '@angular/forms';
import { notBlank } from '../../../../../shared/utils/validation-messages.util';

/** Mirrors backend ClinicRequest validation (@NotBlank, @Email, @Size(min = 8)). */
export const REGISTER_FORM = {
  name: ['', [notBlank]],
  phoneNumber: ['', [notBlank]],
  adminName: ['', [notBlank]],
  adminCi: ['', [notBlank]],
  adminEmail: ['', [Validators.required, Validators.email]],
  adminPassword: ['', [Validators.required, Validators.minLength(8)]]
};
