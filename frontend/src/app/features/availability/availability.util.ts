import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Weekly availability block: when both times are set, the end must be after the start. */
export function availabilityEndAfterStart(control: AbstractControl): ValidationErrors | null {
  const startTime = control.parent?.get('startTime')?.value as string | undefined;
  return startTime && control.value && control.value <= startTime ? { endBeforeStart: true } : null;
}

/** Schedule exception: a partial block needs both times, and the end must be after the start. */
export function exceptionEndAfterStart(control: AbstractControl): ValidationErrors | null {
  const group = control.parent;
  if (!group || group.get('isFullDayBlock')?.value) return null;
  const startTime = group.get('startTime')?.value as string;
  if (!startTime || !control.value) return { rangeRequired: true };
  return control.value <= startTime ? { endBeforeStart: true } : null;
}
