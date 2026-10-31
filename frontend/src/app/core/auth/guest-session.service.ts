import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { Observable, catchError, finalize, map, of, shareReplay, throwError } from 'rxjs';
import { GuestApi } from '../api/guest.api';
import { apiErrorMessage, isRateLimited, retryAfterSeconds } from '../http/api-error';
import { GuestTokenPayload } from '../models/api.models';
import { createCooldown } from '../utils/cooldown.util';

const GUEST_TOKEN_KEY = 'scheduler.guestToken';
const EXPIRY_MARGIN_MS = 30_000;
const UNAVAILABLE_MESSAGE = 'Esta clínica no está disponible para reservas en línea.';

export type GuestSessionStatus = 'loading' | 'ready' | 'unavailable' | 'error';

interface GuestSession {
  token: string;
  clinicSlug: string;
  expiresAt: number;
}

function readStorage(): string | null {
  try {
    return sessionStorage.getItem(GUEST_TOKEN_KEY);
  } catch {
    return null;
  }
}

function writeStorage(value: string | null): void {
  try {
    if (value === null) sessionStorage.removeItem(GUEST_TOKEN_KEY);
    else sessionStorage.setItem(GUEST_TOKEN_KEY, value);
  } catch {}
}

function decodeGuestSession(token: string | null, slug: string): GuestSession | null {
  if (!token) return null;
  try {
    const payload = jwtDecode<GuestTokenPayload>(token);
    if (payload.role !== 'GUEST' || payload.clinicSlug !== slug) return null;
    return { token, clinicSlug: payload.clinicSlug, expiresAt: payload.exp * 1000 };
  } catch {
    return null;
  }
}

function isFresh(session: GuestSession | null): session is GuestSession {
  return !!session && session.expiresAt - EXPIRY_MARGIN_MS > Date.now();
}

@Injectable({ providedIn: 'root' })
export class GuestSessionService {
  private readonly guestApi = inject(GuestApi);

  private slug: string | null = null;
  private session: GuestSession | null = null;
  private pending: Observable<string> | null = null;

  private readonly _status = signal<GuestSessionStatus>('loading');
  private readonly _message = signal<string | null>(null);
  readonly status = this._status.asReadonly();
  readonly message = this._message.asReadonly();
  readonly ready = computed(() => this._status() === 'ready');

  private readonly cooldown = createCooldown();
  readonly coolingDown = this.cooldown.active;
  readonly retryInLabel = this.cooldown.label;
  readonly cooldownFinished = this.cooldown.finished;

  start(slug: string | null): void {
    this.cooldown.reset();
    this.slug = slug?.trim() || null;
    this.pending = null;
    if (!this.slug) {
      this.setStatus('unavailable', UNAVAILABLE_MESSAGE);
      return;
    }
    const stored = decodeGuestSession(readStorage(), this.slug);
    if (isFresh(stored)) {
      this.session = stored;
      this.setStatus('ready');
      return;
    }
    this.clear();
    this.ensureToken().subscribe({ error: () => undefined });
  }

  retry(): void {
    if (this.cooldown.active()) return;
    this.start(this.slug);
  }

  ensureToken(): Observable<string> {
    if (isFresh(this.session)) return of(this.session.token);
    const slug = this.slug;
    if (!slug) return throwError(() => new Error('No guest session'));
    if (this._status() !== 'ready') this.setStatus('loading');
    this.pending ??= this.guestApi.createSession({ slug: slug }).pipe(
      map(({ token }) => {
        const session = decodeGuestSession(token, slug);
        if (!session) throw new Error('Invalid guest token');
        this.session = session;
        writeStorage(token);
        this.setStatus('ready');
        return token;
      }),
      catchError((error: unknown) => {
        this.fail(error);
        return throwError(() => error);
      }),
      finalize(() => (this.pending = null)),
      shareReplay(1),
    );
    return this.pending;
  }

  clear(): void {
    this.session = null;
    writeStorage(null);
  }

  fail(error: unknown): void {
    if (isRateLimited(error)) {
      if (this._status() === 'ready') return;
      const retryAfter = retryAfterSeconds(error);
      if (retryAfter) this.cooldown.start(retryAfter);
    }
    const unavailable =
      error instanceof HttpErrorResponse &&
      (error.status === HttpStatusCode.NotFound || error.status === HttpStatusCode.BadRequest);
    if (unavailable) this.setStatus('unavailable', apiErrorMessage(error, UNAVAILABLE_MESSAGE));
    else this.setStatus('error', apiErrorMessage(error, 'No se pudo iniciar la reserva'));
  }

  private setStatus(status: GuestSessionStatus, message: string | null = null): void {
    this._status.set(status);
    this._message.set(message);
  }
}
