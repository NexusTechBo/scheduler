import { AbstractControl, ValidationErrors, Validators } from '@angular/forms';

const VALIDATION_MESSAGES: Record<string, (error: { requiredLength?: number }) => string> = {
  required: () => 'Este campo es obligatorio',
  email: () => 'Ingresa un email válido',
  minlength: (error) => `Debe tener al menos ${error.requiredLength} caracteres`,
  maxlength: (error) => `Debe tener como máximo ${error.requiredLength} caracteres`,
  pattern: () => 'El formato ingresado no es válido',
};

export function getValidationErrorMessage(errors: ValidationErrors | null): string | null {
  if (!errors) return null;

  const key = Object.keys(errors)[0];
  return VALIDATION_MESSAGES[key]?.(errors[key]) ?? 'Campo inválido';
}

export function notBlank(control: AbstractControl): ValidationErrors | null {
  return String(control.value ?? '').trim() ? null : { required: true };
}

export const ciValidator = Validators.pattern(/^\d+$/);
export const CI_ERRORS = { pattern: 'Solo números' };

export const phoneValidator = Validators.pattern(/^[\d\s()+-]{7,20}$/);
export const PHONE_ERRORS = {
  pattern: 'De 7 a 20 caracteres: números, espacios, +, - o paréntesis',
};

export function specialtyForDoctor(control: AbstractControl): ValidationErrors | null {
  return control.parent?.get('role')?.value === 'DOCTOR' && !control.value
    ? { required: true }
    : null;
}

export function currentPasswordWhenChanging(control: AbstractControl): ValidationErrors | null {
  return control.parent?.get('newPassword')?.value && !control.value ? { required: true } : null;
}
