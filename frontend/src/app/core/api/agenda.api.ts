import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Availability,
  AvailabilityRequest,
  AvailableSlots,
  ScheduleException,
  ScheduleExceptionRequest,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params.util';

@Injectable({ providedIn: 'root' })
export class AgendaApi {
  private readonly http = inject(HttpClient);

  availabilities(): Observable<Availability[]> {
    return this.http.get<Availability[]>('doctorAvailability/me');
  }

  addAvailability(request: AvailabilityRequest): Observable<Availability> {
    return this.http.post<Availability>('doctorAvailability', request);
  }

  removeAvailability(id: number): Observable<void> {
    return this.http.delete<void>(`doctorAvailability/${id}`);
  }

  availableSlots(doctorId: number, date: string): Observable<AvailableSlots> {
    return this.http.get<AvailableSlots>(`doctorAvailability/doctor/${doctorId}/availables`, {
      params: toHttpParams({ date }),
    });
  }

  exceptions(from?: string): Observable<ScheduleException[]> {
    return this.http.get<ScheduleException[]>('scheduleException', {
      params: toHttpParams({ from }),
    });
  }

  addException(request: ScheduleExceptionRequest): Observable<ScheduleException> {
    return this.http.post<ScheduleException>('scheduleException', request);
  }

  removeException(id: number): Observable<void> {
    return this.http.delete<void>(`scheduleException/${id}`);
  }
}
