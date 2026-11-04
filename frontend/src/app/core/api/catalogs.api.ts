import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Specialty, SpecialtyRequest } from '../models/api.models';
import { byName } from '../utils/sort.util';

@Injectable({ providedIn: 'root' })
export class CatalogsApi {
  private readonly http = inject(HttpClient);

  specialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>('specialties').pipe(map((list) => [...list].sort(byName)));
  }

  createSpecialty(request: SpecialtyRequest): Observable<Specialty> {
    return this.http.post<Specialty>('specialties', request);
  }
}
