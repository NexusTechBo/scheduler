import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Page,
  PageQuery,
  Patient,
  PatientRegisterRequest,
  PatientUpdateRequest,
  Staff,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params.util';

@Injectable({ providedIn: 'root' })
export class PatientsApi {
  private readonly http = inject(HttpClient);

  list(query: PageQuery = {}): Observable<Page<Patient>> {
    return this.http.get<Page<Patient>>('patients', { params: toHttpParams(query) });
  }

  create(request: PatientRegisterRequest): Observable<Patient> {
    return this.http.post<Patient>('patients', request);
  }

  update(id: number, request: PatientUpdateRequest): Observable<Patient> {
    return this.http.put<Patient>(`patients/update/${id}`, request);
  }

  updateSelf(request: PatientUpdateRequest): Observable<Patient> {
    return this.http.put<Patient>('patients/update', request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`patients/${id}`);
  }

  doctors(patientId: number): Observable<Staff[]> {
    return this.http.get<Staff[]>(`patients/${patientId}/doctors`);
  }
}
