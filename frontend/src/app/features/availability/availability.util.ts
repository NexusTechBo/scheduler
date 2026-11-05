import { AbstractControl, ValidationErrors } from '@angular/forms';

export function availabilityEndAfterStart(control: AbstractControl): ValidationErrors | null {
  const startTime = control.parent?.get('startTime')?.value as string | undefined;
  return startTime && control.value && control.value <= startTime ? { endBeforeStart: true } : null;
}

export function exceptionEndAfterStart(control: AbstractControl): ValidationErrors | null {
  const group = control.parent;
  if (!group || group.get('isFullDayBlock')?.value) return null;
  const startTime = group.get('startTime')?.value as string;
  if (!startTime || !control.value) return { rangeRequired: true };
  return control.value <= startTime ? { endBeforeStart: true } : null;
}
