import { HttpParams } from '@angular/common/http';

type ParamValue = string | number | boolean | null | undefined;

export function toHttpParams(query: object = {}): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query) as [string, ParamValue][]) {
    if (value === null || value === undefined || value === '') continue;
    params = params.set(key, String(value));
  }
  return params;
}
