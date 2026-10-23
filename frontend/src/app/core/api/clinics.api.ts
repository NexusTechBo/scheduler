import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ActivateClinicRequest,
  ClinicActivatedResponse,
  ClinicCreatedResponse,
  ClinicRequest,
  ResendActivationRequest,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClinicsApi {
  private readonly http = inject(HttpClient);

  create(request: ClinicRequest): Observable<ClinicCreatedResponse> {
    return this.http.post<ClinicCreatedResponse>('clinics', request);
  }

  activate(token: string): Observable<ClinicActivatedResponse> {
    const request: ActivateClinicRequest = { token };
    return this.http.post<ClinicActivatedResponse>('clinics/activate', request);
  }

  resendActivation(email: string): Observable<void> {
    const request: ResendActivationRequest = { email };
    return this.http.post<void>('clinics/activation/resend', request);
  }
}
