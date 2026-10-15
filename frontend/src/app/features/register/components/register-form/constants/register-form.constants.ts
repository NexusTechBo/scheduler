import { Validators } from '@angular/forms';

/** Mirrors backend ClinicRequest validation (@NotBlank, @Email, @Size(min = 8)). */
export const REGISTER_FORM = {
  name: ['', [Validators.required]],
  phoneNumber: ['', [Validators.required]],
  adminName: ['', [Validators.required]],
  adminCi: ['', [Validators.required]],
  adminEmail: ['', [Validators.required, Validators.email]],
  adminPassword: ['', [Validators.required, Validators.minLength(8)]]
};
