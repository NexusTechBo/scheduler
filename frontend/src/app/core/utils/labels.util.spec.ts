import { firstName, initials, rolesLabel, specialtyLabel } from './labels.util';

describe('labels.util', () => {
  describe('rolesLabel', () => {
    it('joins role labels in a fixed order', () => {
      expect(rolesLabel(['DOCTOR', 'ADMINISTRATOR'])).toBe('Administrador · Doctor');
    });

    it('returns an empty string without roles', () => {
      expect(rolesLabel([])).toBe('');
    });
  });

  describe('specialtyLabel', () => {
    it('returns the name or a placeholder', () => {
      expect(specialtyLabel('Cardiología')).toBe('Cardiología');
      expect(specialtyLabel(null)).toBe('Sin especialidad');
      expect(specialtyLabel(undefined)).toBe('Sin especialidad');
    });
  });

  describe('firstName', () => {
    it('skips the doctor title', () => {
      expect(firstName('Dra. Lucía Ruiz')).toBe('Lucía');
      expect(firstName('dr Juan Gómez')).toBe('Juan');
    });

    it('returns the first word of a plain name', () => {
      expect(firstName('Ana Pérez')).toBe('Ana');
    });
  });

  describe('initials', () => {
    it('builds two uppercase initials skipping the title', () => {
      expect(initials('Dr. juan gómez')).toBe('JG');
    });

    it('uses one initial for a single word', () => {
      expect(initials('Ana')).toBe('A');
    });

    it('returns a placeholder for empty names', () => {
      expect(initials(null)).toBe('?');
      expect(initials('')).toBe('?');
      expect(initials('   ')).toBe('?');
    });
  });
});
