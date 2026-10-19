import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Clinic, ClinicCreatedResponse, ClinicRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClinicsApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Clinic[]> {
    return this.http.get<Clinic[]>('clinics');
  }

  create(request: ClinicRequest): Observable<ClinicCreatedResponse> {
    return this.http.post<ClinicCreatedResponse>('clinics', request);
  }
}
