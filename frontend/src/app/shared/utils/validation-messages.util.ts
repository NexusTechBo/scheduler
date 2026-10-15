import { AbstractControl, ValidationErrors } from '@angular/forms';

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

/** Message to show under a field, only once the user has touched it. */
export function fieldError(control: AbstractControl): string | null {
  return control.touched ? getValidationErrorMessage(control.errors) : null;
}
