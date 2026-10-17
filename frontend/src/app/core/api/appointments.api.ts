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

  book(request: AppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>('appointments', request);
  }

  confirm(id: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`appointments/${id}/confirm`, null);
  }

  cancel(id: number, reason: string): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`appointments/${id}/cancel`, { reason });
  }

  board(query: {
    from: string;
    to: string;
    doctorId?: number;
    patientId?: number;
  }): Observable<AppointmentBoard> {
    return this.http.get<AppointmentBoard>('appointments/board', { params: toHttpParams(query) });
  }

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
