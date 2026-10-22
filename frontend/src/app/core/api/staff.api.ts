import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AssignPatientRequest,
  Page,
  PageQuery,
  Patient,
  ProfileUpdateRequest,
  Staff,
  StaffFilters,
  StaffRegisterRequest,
  StaffUpdateRequest,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params.util';

@Injectable({ providedIn: 'root' })
export class StaffApi {
  private readonly http = inject(HttpClient);

  doctors(
    query: { specialtyId?: number; isActive?: boolean } & PageQuery = {},
  ): Observable<Page<Staff>> {
    return this.http.get<Page<Staff>>('personal/doctors', { params: toHttpParams(query) });
  }

  list(filters: StaffFilters = {}): Observable<Page<Staff>> {
    return this.http.get<Page<Staff>>('personal', { params: toHttpParams(filters) });
  }

  create(request: StaffRegisterRequest): Observable<Staff> {
    return this.http.post<Staff>('personal', request);
  }

  update(id: number, request: StaffUpdateRequest): Observable<Staff> {
    return this.http.put<Staff>(`personal/update/${id}`, request);
  }

  updateSelf(request: ProfileUpdateRequest): Observable<Staff> {
    return this.http.put<Staff>('personal/update', request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`personal/${id}`);
  }

  assignPatient(request: AssignPatientRequest): Observable<void> {
    return this.http.post<void>('personal/patients/assign', request);
  }

  removePatient(request: AssignPatientRequest): Observable<void> {
    return this.http.delete<void>('personal/patients/remove', { body: request });
  }

  patientsOf(doctorId: number): Observable<Patient[]> {
    return this.http.get<Patient[]>(`personal/${doctorId}/patients`);
  }
}
