import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, map, switchMap, throwError } from 'rxjs';
import { WhatsappSessionStatusResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class WhatsappApi {
  private readonly http = inject(HttpClient);

  qr(): Observable<Blob | null> {
    return this.http.get('whatsapp/qr', { responseType: 'blob', observe: 'response' }).pipe(
      map((response) => (response.status === HttpStatusCode.NoContent ? null : response.body)),
      catchError(withJsonErrorBody),
    );
  }

  status(): Observable<WhatsappSessionStatusResponse> {
    return this.http.get<WhatsappSessionStatusResponse>('whatsapp/status');
  }

  logout(): Observable<void> {
    return this.http.post<void>('whatsapp/logout', null);
  }
}

function withJsonErrorBody(error: unknown): Observable<never> {
  if (!(error instanceof HttpErrorResponse) || !(error.error instanceof Blob)) {
    return throwError(() => error);
  }
  return from(error.error.text()).pipe(
    switchMap((text) => {
      let body: unknown = null;
      try {
        body = JSON.parse(text);
      } catch {
        body = null;
      }
      return throwError(
        () =>
          new HttpErrorResponse({
            error: body,
            headers: error.headers,
            status: error.status,
            url: error.url ?? undefined,
          }),
      );
    }),
  );
}
