import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  catchError,
  concat,
  defer,
  distinctUntilChanged,
  exhaustMap,
  finalize,
  from,
  ignoreElements,
  map,
  repeat,
  switchMap,
  takeWhile,
  tap,
  throwError,
  timer,
} from 'rxjs';
import { WhatsappApi } from '../../../../core/api/whatsapp.api';
import { apiErrorMessage } from '../../../../core/http/api-error';
import {
  WhatsappSessionStatus,
  WhatsappSessionStatusResponse,
} from '../../../../core/models/api.models';
import { NotificationService } from '../../../../shared/services/notification.service';

const STATUS_POLL_MS = 3_000;
const QR_REFRESH_MS = 20_000;
const QR_MAX_CODES = 6;

export type WhatsappView =
  'loading' | 'connected' | 'linking' | 'reconnecting' | 'expired' | 'error';

const WHATSAPP_STATUS_LABEL: Record<WhatsappSessionStatus, string> = {
  IDLE: 'Sin vincular',
  INITIALIZING: 'Iniciando…',
  QR_PENDING: 'Esperando escaneo',
  READY: 'Conectado',
  DISCONNECTED: 'Reconectando…',
};

@Injectable()
export class WhatsappQrService {
  private readonly api = inject(WhatsappApi);
  private readonly notifications = inject(NotificationService);

  private readonly run = signal(0);

  private readonly _session = signal<WhatsappSessionStatusResponse | null>(null);
  private readonly _qrUrl = signal<string | null>(null);
  private readonly _error = signal<string | null>(null);
  private readonly _expired = signal(false);
  private readonly _loggingOut = signal(false);

  readonly session = this._session.asReadonly();
  readonly qrUrl = this._qrUrl.asReadonly();
  readonly error = this._error.asReadonly();
  readonly expired = this._expired.asReadonly();
  readonly loggingOut = this._loggingOut.asReadonly();

  readonly status = computed(() => this.session()?.status ?? null);
  readonly statusLabel = computed(() => {
    const status = this.status();
    return status ? WHATSAPP_STATUS_LABEL[status] : '';
  });
  readonly clinicPhoneNumber = computed(() => this.session()?.clinicPhoneNumber ?? null);
  readonly view = computed<WhatsappView>(() => {
    if (this.error()) return 'error';
    const status = this.status();
    if (!status) return 'loading';
    if (status === 'READY') return 'connected';
    if (status === 'DISCONNECTED') return 'reconnecting';
    return this.expired() ? 'expired' : 'linking';
  });

  constructor() {
    toObservable(this.run)
      .pipe(
        switchMap(() => this.pollStatus()),
        takeUntilDestroyed(),
      )
      .subscribe();

    toObservable(computed(() => this.view() === 'linking'))
      .pipe(
        distinctUntilChanged(),
        switchMap((linking) => {
          if (linking) return this.refreshQr();
          this.setQr(null);
          return EMPTY;
        }),
        takeUntilDestroyed(),
      )
      .subscribe();

    inject(DestroyRef).onDestroy(() => this.setQr(null));
  }

  restart(): void {
    this._error.set(null);
    this._expired.set(false);
    this._session.set(null);
    this.run.update((n) => n + 1);
  }

  logout(): Observable<void> {
    this._loggingOut.set(true);
    return this.api.logout().pipe(
      tap(() => this.restart()),
      finalize(() => this._loggingOut.set(false)),
    );
  }

  private pollStatus(): Observable<never> {
    return timer(0, STATUS_POLL_MS).pipe(
      exhaustMap(() => this.api.status()),
      takeWhile((session) => session.status !== 'READY' && !this.expired(), true),
      tap((session) => this.onStatus(session)),
      ignoreElements(),
      catchError((error) => this.fail(error, 'No se pudo consultar el estado de WhatsApp')),
    );
  }

  private refreshQr(): Observable<never> {
    return concat(
      defer(() => this.fetchQr()).pipe(
        tap((qr) => this.setQr(qr)),
        repeat({ count: QR_MAX_CODES, delay: QR_REFRESH_MS }),
      ),
      timer(QR_REFRESH_MS).pipe(tap(() => this._expired.set(true))),
    ).pipe(
      ignoreElements(),
      catchError((error) => this.fail(error, 'No se pudo generar el código QR')),
    );
  }

  private fetchQr(): Observable<Blob | null> {
    return this.api.qr().pipe(
      map((response) => (response.status === HttpStatusCode.NoContent ? null : response.body)),
      catchError(withJsonErrorBody),
    );
  }

  private onStatus(session: WhatsappSessionStatusResponse): void {
    const previous = this.status();
    this._session.set(session);
    if (session.status === 'READY' && previous && previous !== 'READY') {
      this.notifications.success('WhatsApp vinculado correctamente');
    }
  }

  private fail(error: unknown, fallback: string): Observable<never> {
    this._error.set(apiErrorMessage(error, fallback));
    return EMPTY;
  }

  private setQr(qr: Blob | null): void {
    const previous = this._qrUrl();
    if (previous) URL.revokeObjectURL(previous);
    this._qrUrl.set(qr ? URL.createObjectURL(qr) : null);
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
