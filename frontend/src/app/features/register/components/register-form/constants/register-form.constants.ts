import { Validators } from '@angular/forms';
import { notBlank } from '../../../../../shared/utils/validation-messages.util';

export const REGISTER_FORM = {
  name: ['', [notBlank]],
  phoneNumber: ['', [notBlank]],
  adminName: ['', [notBlank]],
  adminCi: ['', [notBlank]],
  adminEmail: ['', [Validators.required, Validators.email]],
  adminPassword: ['', [Validators.required, Validators.minLength(8)]],
};
