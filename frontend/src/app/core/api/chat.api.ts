import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ChatApi {
  private readonly http = inject(HttpClient);

  send(message: string): Observable<string> {
    return this.http.post('chat/patient', message, {
      headers: { 'Content-Type': 'text/plain' },
      responseType: 'text'
    });
  }
}
