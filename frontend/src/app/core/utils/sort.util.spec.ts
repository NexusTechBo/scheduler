import { byName } from './sort.util';

describe('byName', () => {
  it('sorts items alphabetically by name', () => {
    const items = [{ name: 'Pediatría' }, { name: 'Cardiología' }, { name: 'Neurología' }];

    const sorted = [...items].sort(byName);

    expect(sorted.map((item) => item.name)).toEqual(['Cardiología', 'Neurología', 'Pediatría']);
  });

  it('returns 0 for equal names', () => {
    expect(byName({ name: 'Dermatología' }, { name: 'Dermatología' })).toBe(0);
  });
});
