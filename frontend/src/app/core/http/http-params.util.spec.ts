import { toHttpParams } from './http-params.util';

describe('toHttpParams', () => {
  it('converts values to strings', () => {
    const params = toHttpParams({ page: 0, active: false, search: 'ana' });
    expect(params.get('page')).toBe('0');
    expect(params.get('active')).toBe('false');
    expect(params.get('search')).toBe('ana');
  });

  it('skips null, undefined and empty strings', () => {
    const params = toHttpParams({ a: null, b: undefined, c: '' });
    expect(params.keys()).toEqual([]);
  });

  it('returns empty params without a query', () => {
    expect(toHttpParams().keys()).toEqual([]);
  });
});
