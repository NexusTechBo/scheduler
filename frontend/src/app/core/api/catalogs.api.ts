import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Specialty } from '../models/api.models';

const PLACEHOLDER_SPECIALTY = 'None';

@Injectable({ providedIn: 'root' })
export class CatalogsApi {
  private readonly http = inject(HttpClient);

  /** The tenant migration seeds a placeholder specialty "None" (id 1) that must never be offered to users. */
  specialties(): Observable<Specialty[]> {
    return this.http
      .get<Specialty[]>('specialties')
      .pipe(
        map((list) =>
          list
            .filter((specialty) => specialty.name !== PLACEHOLDER_SPECIALTY)
            .sort((a, b) => a.name.localeCompare(b.name)),
        ),
      );
  }

  /** ADMINISTRATOR only. */
  createSpecialty(name: string): Observable<Specialty> {
    return this.http.post<Specialty>('specialties', { name });
  }
}
