import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Specialty, SpecialtyRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CatalogsApi {
  private readonly http = inject(HttpClient);

  specialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>('specialties');
  }

  createSpecialty(request: SpecialtyRequest): Observable<Specialty> {
    return this.http.post<Specialty>('specialties', request);
  }
}
