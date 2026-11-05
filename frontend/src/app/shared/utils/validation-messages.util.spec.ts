import { FormControl } from '@angular/forms';
import { CI_ERRORS, ciValidator, getValidationErrorMessage } from './validation-messages.util';

function ciErrors(value: string) {
  return ciValidator(new FormControl(value));
}

describe('validation-messages util', () => {
  describe('ciValidator (DEF-02)', () => {
    it.each(['12345', '1234567', '1234567890'])('accepts "%s"', (value) => {
      expect(ciErrors(value)).toBeNull();
    });

    it.each([
      ['1234', 'fewer than 5 digits'],
      ['12345678901', 'more than 10 digits'],
      ['12a45', 'letters'],
      ['12 345', 'spaces'],
      ['12345-LP', 'an extension suffix'],
      [' 12345', 'leading whitespace'],
    ])('rejects "%s" (%s) with a pattern error', (value) => {
      expect(ciErrors(value)).toEqual({ pattern: expect.anything() });
    });

    it('leaves empty values to the required validators', () => {
      expect(ciErrors('')).toBeNull();
    });
  });

  describe('CI_ERRORS', () => {
    it('describes the CI rule in Spanish', () => {
      expect(CI_ERRORS.pattern).toBe('Solo números, de 5 a 10 dígitos');
    });
  });

  describe('getValidationErrorMessage', () => {
    it('returns null without errors', () => {
      expect(getValidationErrorMessage(null)).toBeNull();
    });

    it('uses the generic pattern message when no override is given', () => {
      expect(getValidationErrorMessage({ pattern: {} })).toBe('El formato ingresado no es válido');
    });

    it('includes the required length for maxlength', () => {
      expect(getValidationErrorMessage({ maxlength: { requiredLength: 10 } })).toBe(
        'Debe tener como máximo 10 caracteres',
      );
    });
  });
});
