import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../http/http-params.util';
import {
  AvailableSlots,
  GuestAppointmentRequest,
  GuestAppointmentResponse,
  GuestClinic,
  GuestDoctor,
  GuestSessionResponse,
  Specialty,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class GuestApi {
  private readonly http = inject(HttpClient);

  createSession(slug: string): Observable<GuestSessionResponse> {
    return this.http.post<GuestSessionResponse>('public/guest/session', { slug });
  }

  clinic(): Observable<GuestClinic> {
    return this.http.get<GuestClinic>('guest/clinic');
  }

  specialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>('guest/specialties');
  }

  doctors(specialtyId?: number): Observable<GuestDoctor[]> {
    return this.http.get<GuestDoctor[]>('guest/doctors', {
      params: toHttpParams({ specialtyId }),
    });
  }

  availableSlots(doctorId: number, date: string): Observable<AvailableSlots> {
    return this.http.get<AvailableSlots>(`guest/doctors/${doctorId}/availables`, {
      params: toHttpParams({ date }),
    });
  }

  book(request: GuestAppointmentRequest): Observable<GuestAppointmentResponse> {
    return this.http.post<GuestAppointmentResponse>('guest/appointments', request);
  }
}
