import { FormControl, FormGroup } from '@angular/forms';
import { currentPasswordWhenChanging } from '../../features/profile/profile-page.component';
import { specialtyForDoctor } from '../../features/staff/components/staff-form-dialog/staff-form-dialog.component';
import { ciValidator, phoneValidator } from './validation-messages.util';

describe('form validators', () => {
  const valid = (validator: typeof ciValidator, value: string) =>
    validator(new FormControl(value)) === null;

  it('ciValidator accepts digits only', () => {
    expect(valid(ciValidator, '1234567')).toBeTrue();
    expect(valid(ciValidator, '')).toBeTrue();
    expect(valid(ciValidator, '12a45')).toBeFalse();
    expect(valid(ciValidator, '123-45')).toBeFalse();
  });

  it('phoneValidator accepts 7 to 20 digits, spaces, +, - or parentheses', () => {
    expect(valid(phoneValidator, '+591 (2) 222-3344')).toBeTrue();
    expect(valid(phoneValidator, '')).toBeTrue();
    expect(valid(phoneValidator, '123456')).toBeFalse();
    expect(valid(phoneValidator, '1'.repeat(21))).toBeFalse();
    expect(valid(phoneValidator, '7777777x')).toBeFalse();
  });

  it('requires a specialty only for doctors', () => {
    const form = new FormGroup({
      role: new FormControl('DOCTOR'),
      specialtyId: new FormControl<number | null>(null, specialtyForDoctor),
    });
    const specialty = form.controls.specialtyId;
    specialty.updateValueAndValidity();
    expect(specialty.hasError('required')).toBeTrue();

    specialty.setValue(3);
    expect(specialty.valid).toBeTrue();

    specialty.setValue(null);
    form.controls.role.setValue('ASSISTANT');
    specialty.updateValueAndValidity();
    expect(specialty.valid).toBeTrue();
  });

  it('requires the current password only when a new one is set', () => {
    const form = new FormGroup({
      currentPassword: new FormControl('', currentPasswordWhenChanging),
      newPassword: new FormControl(''),
    });
    const current = form.controls.currentPassword;
    expect(current.valid).toBeTrue();

    form.controls.newPassword.setValue('nueva-clave');
    current.updateValueAndValidity();
    expect(current.hasError('required')).toBeTrue();

    current.setValue('actual');
    expect(current.valid).toBeTrue();
  });
});
