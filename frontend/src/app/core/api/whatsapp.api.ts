import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { WhatsappSessionStatusResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class WhatsappApi {
  private readonly http = inject(HttpClient);

  qr(): Observable<HttpResponse<Blob>> {
    return this.http.get('whatsapp/qr', { responseType: 'blob', observe: 'response' });
  }

  status(): Observable<WhatsappSessionStatusResponse> {
    return this.http.get<WhatsappSessionStatusResponse>('whatsapp/status');
  }

  logout(): Observable<void> {
    return this.http.post<void>('whatsapp/logout', null);
  }
}
