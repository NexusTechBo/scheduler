import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** AI scheduling assistant (Spring AI + Gemini). PATIENT only; the backend keeps the conversation memory. */
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
