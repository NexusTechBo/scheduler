import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../http/http-params.util';
import { Page, Patient, PatientFilters, PatientRequest, Staff } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PatientsApi {
  private readonly http = inject(HttpClient);

  list(query: PatientFilters = {}): Observable<Page<Patient>> {
    return this.http.get<Page<Patient>>('patients', { params: toHttpParams(query) });
  }

  create(request: PatientRequest): Observable<Patient> {
    return this.http.post<Patient>('patients', request);
  }

  update(id: number, request: PatientRequest): Observable<Patient> {
    return this.http.put<Patient>(`patients/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`patients/${id}`);
  }

  activate(id: number): Observable<Patient> {
    return this.http.patch<Patient>(`patients/${id}/activate`, null);
  }

  doctors(patientId: number): Observable<Staff[]> {
    return this.http.get<Staff[]>(`patients/${patientId}/doctors`);
  }
}
