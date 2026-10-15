import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AppointmentBoard,
  AppointmentCalendar,
  AppointmentFilters,
  AppointmentRequest,
  AppointmentResponse,
  Page,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params.util';

/**
 * For DOCTOR and PATIENT callers the backend forces doctorId/patientId to the caller's own id,
 * so the same calls work for every role.
 */
@Injectable({ providedIn: 'root' })
export class AppointmentsApi {
  private readonly http = inject(HttpClient);

  list(filters: AppointmentFilters = {}): Observable<Page<AppointmentResponse>> {
    return this.http.get<Page<AppointmentResponse>>('appointments', {
      params: toHttpParams(filters),
    });
  }

  get(id: number): Observable<AppointmentResponse> {
    return this.http.get<AppointmentResponse>(`appointments/${id}`);
  }

  /** PATIENT bookings start PENDING, staff bookings are CONFIRMED directly. */
  book(request: AppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>('appointments', request);
  }

  /** DOCTOR / ASSISTANT only. */
  confirm(id: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`appointments/${id}/confirm`, null);
  }

  /** DOCTOR / ASSISTANT only. `reason` is optional and shown to the patient. */
  cancel(id: number, reason?: string | null): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`appointments/${id}/cancel`, {
      reason: reason || null,
    });
  }

  /** `from`/`to` are inclusive ISO dates. */
  board(query: {
    from: string;
    to: string;
    doctorId?: number;
    patientId?: number;
  }): Observable<AppointmentBoard> {
    return this.http.get<AppointmentBoard>('appointments/board', { params: toHttpParams(query) });
  }

  /** `month` is 1-12. */
  calendar(query: {
    month: number;
    year: number;
    doctorId?: number;
    patientId?: number;
  }): Observable<AppointmentCalendar> {
    return this.http.get<AppointmentCalendar>('appointments/calendar', {
      params: toHttpParams(query),
    });
  }
}
