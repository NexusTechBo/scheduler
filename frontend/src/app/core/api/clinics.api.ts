import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ClinicCreatedResponse, ClinicRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClinicsApi {
  private readonly http = inject(HttpClient);

  create(request: ClinicRequest): Observable<ClinicCreatedResponse> {
    return this.http.post<ClinicCreatedResponse>('clinics', request);
  }
}
