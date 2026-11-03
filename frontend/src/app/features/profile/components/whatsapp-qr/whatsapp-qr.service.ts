import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import {
  EMPTY,
  Observable,
  catchError,
  concat,
  defer,
  distinctUntilChanged,
  exhaustMap,
  finalize,
  ignoreElements,
  repeat,
  switchMap,
  takeWhile,
  tap,
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
  private readonly sanitizer = inject(DomSanitizer);
  private readonly notifications = inject(NotificationService);

  private readonly run = signal(0);
  private objectUrl: string | null = null;

  readonly session = signal<WhatsappSessionStatusResponse | null>(null);
  readonly qrUrl = signal<SafeUrl | null>(null);
  readonly error = signal<string | null>(null);
  readonly expired = signal(false);
  readonly loggingOut = signal(false);

  readonly status = computed(() => this.session()?.status ?? null);
  readonly statusLabel = computed(() => {
    const status = this.status();
    return status ? WHATSAPP_STATUS_LABEL[status] : '';
  });
  readonly phoneNumber = computed(() => {
    const id = this.session()?.id;
    return id ? `+${id}` : null;
  });
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
    this.error.set(null);
    this.expired.set(false);
    this.session.set(null);
    this.run.update((n) => n + 1);
  }

  logout(): Observable<void> {
    this.loggingOut.set(true);
    return this.api.logout().pipe(
      tap(() => this.restart()),
      finalize(() => this.loggingOut.set(false)),
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
      defer(() => this.api.qr()).pipe(
        tap((qr) => this.setQr(qr)),
        repeat({ count: QR_MAX_CODES, delay: QR_REFRESH_MS }),
      ),
      timer(QR_REFRESH_MS).pipe(tap(() => this.expired.set(true))),
    ).pipe(
      ignoreElements(),
      catchError((error) => this.fail(error, 'No se pudo generar el código QR')),
    );
  }

  private onStatus(session: WhatsappSessionStatusResponse): void {
    const previous = this.status();
    this.session.set(session);
    if (session.status === 'READY' && previous && previous !== 'READY') {
      this.notifications.success('WhatsApp vinculado correctamente');
    }
  }

  private fail(error: unknown, fallback: string): Observable<never> {
    this.error.set(apiErrorMessage(error, fallback));
    return EMPTY;
  }

  private setQr(qr: Blob | null): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = qr ? URL.createObjectURL(qr) : null;
    this.qrUrl.set(this.objectUrl ? this.sanitizer.bypassSecurityTrustUrl(this.objectUrl) : null);
  }
}
