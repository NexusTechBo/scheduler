import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { GuestApi } from '../api/guest.api';
import { ApiError, GuestSessionResponse, GuestTokenPayload } from '../models/api.models';
import { GuestSessionService } from './guest-session.service';

const GUEST_TOKEN_KEY = 'scheduler.guestToken';
const SLUG = 'clinicacentral';
const UNAVAILABLE_MESSAGE = 'Esta clínica no está disponible para reservas en línea.';
const FALLBACK_MESSAGE = 'No se pudo iniciar la reserva';
const RATE_LIMIT_MESSAGE =
  'Has realizado demasiadas solicitudes. Intenta de nuevo en unos minutos.';

function base64Url(value: object): string {
  return btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function guestToken(overrides: Partial<GuestTokenPayload> = {}): string {
  const payload: GuestTokenPayload = {
    sub: 'guest',
    role: 'GUEST',
    clinicId: 1,
    clinicName: 'Clinica Central',
    clinicSlug: SLUG,
    exp: Math.floor(Date.now() / 1000) + 3600,
    ...overrides,
  };
  return `${base64Url({ alg: 'HS256', typ: 'JWT' })}.${base64Url(payload)}.signature`;
}

function sessionResponse(token = guestToken()): GuestSessionResponse {
  return { token, expiresAt: new Date(Date.now() + 3600_000).toISOString() };
}

function httpError(status: number, body: Partial<ApiError> | null = null, headers?: HttpHeaders) {
  return new HttpErrorResponse({ status, headers, error: body });
}

describe('GuestSessionService', () => {
  let service: GuestSessionService;
  let guestApi: { createSession: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    sessionStorage.clear();
    guestApi = { createSession: vi.fn(() => of(sessionResponse())) };
    TestBed.configureTestingModule({ providers: [{ provide: GuestApi, useValue: guestApi }] });
    service = TestBed.inject(GuestSessionService);
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.useRealTimers();
  });

  function failWith(error: unknown): void {
    guestApi.createSession.mockReturnValue(throwError(() => error));
    service.start(SLUG);
  }

  describe('start', () => {
    it('marks the session unavailable without calling the API when there is no slug', () => {
      service.start('   ');

      expect(service.status()).toBe('unavailable');
      expect(service.message()).toBe(UNAVAILABLE_MESSAGE);
      expect(guestApi.createSession).not.toHaveBeenCalled();
    });

    it('creates a session for the trimmed slug and stores the token', () => {
      const token = guestToken();
      guestApi.createSession.mockReturnValue(of(sessionResponse(token)));

      service.start(`  ${SLUG}  `);

      expect(guestApi.createSession).toHaveBeenCalledWith({ slug: SLUG });
      expect(service.status()).toBe('ready');
      expect(service.ready()).toBe(true);
      expect(service.message()).toBeNull();
      expect(sessionStorage.getItem(GUEST_TOKEN_KEY)).toBe(token);
    });

    it('reuses the created token without a second request', () => {
      const token = guestToken();
      guestApi.createSession.mockReturnValue(of(sessionResponse(token)));
      service.start(SLUG);

      let received: string | undefined;
      service.ensureToken().subscribe((value) => (received = value));

      expect(received).toBe(token);
      expect(guestApi.createSession).toHaveBeenCalledOnce();
    });

    it('restores a fresh stored token for the same clinic without calling the API', () => {
      const token = guestToken();
      sessionStorage.setItem(GUEST_TOKEN_KEY, token);

      service.start(SLUG);

      expect(service.status()).toBe('ready');
      expect(guestApi.createSession).not.toHaveBeenCalled();
    });

    it('replaces a stored token of another clinic with a new session', () => {
      sessionStorage.setItem(GUEST_TOKEN_KEY, guestToken({ clinicSlug: 'otra' }));
      const token = guestToken();
      guestApi.createSession.mockReturnValue(of(sessionResponse(token)));

      service.start(SLUG);

      expect(guestApi.createSession).toHaveBeenCalledOnce();
      expect(sessionStorage.getItem(GUEST_TOKEN_KEY)).toBe(token);
    });

    it('replaces an expired stored token with a new session', () => {
      sessionStorage.setItem(
        GUEST_TOKEN_KEY,
        guestToken({ exp: Math.floor(Date.now() / 1000) - 10 }),
      );

      service.start(SLUG);

      expect(guestApi.createSession).toHaveBeenCalledOnce();
      expect(service.status()).toBe('ready');
    });

    it('stays loading while the session request is in flight', () => {
      guestApi.createSession.mockReturnValue(new Subject<GuestSessionResponse>());

      service.start(SLUG);

      expect(service.status()).toBe('loading');
      expect(service.ready()).toBe(false);
    });

    it('shares a single in-flight request between concurrent callers', () => {
      const response = new Subject<GuestSessionResponse>();
      guestApi.createSession.mockReturnValue(response);
      service.start(SLUG);

      const received: string[] = [];
      service.ensureToken().subscribe((token) => received.push(token));
      const token = guestToken();
      response.next(sessionResponse(token));
      response.complete();

      expect(guestApi.createSession).toHaveBeenCalledOnce();
      expect(received).toEqual([token]);
    });

    it('reports an error when the returned token belongs to another clinic', () => {
      guestApi.createSession.mockReturnValue(
        of(sessionResponse(guestToken({ clinicSlug: 'otra' }))),
      );

      service.start(SLUG);

      expect(service.status()).toBe('error');
      expect(service.message()).toBe(FALLBACK_MESSAGE);
      expect(sessionStorage.getItem(GUEST_TOKEN_KEY)).toBeNull();
    });
  });

  describe('ensureToken', () => {
    it('fails without a request when the service was never started with a slug', () => {
      let error: unknown;
      service.ensureToken().subscribe({ error: (e) => (error = e) });

      expect(error).toBeInstanceOf(Error);
      expect(guestApi.createSession).not.toHaveBeenCalled();
    });

    it('rethrows the HTTP error to the subscriber after updating the state', () => {
      const failure = httpError(404);
      failWith(failure);

      let error: unknown;
      service.ensureToken().subscribe({ error: (e) => (error = e) });

      expect(error).toBe(failure);
      expect(service.status()).toBe('unavailable');
    });
  });

  describe('fail (DEF-01)', () => {
    it('shows the fixed unavailable message for a 400 instead of the field validation text', () => {
      failWith(
        httpError(400, {
          status: 400,
          message: 'Validation failed',
          errors: [{ field: 'slug', message: 'debe tener entre 3 y 50 caracteres' }],
        }),
      );

      expect(service.status()).toBe('unavailable');
      expect(service.message()).toBe(UNAVAILABLE_MESSAGE);
      expect(service.message()).not.toContain('caracteres');
      expect(service.message()).not.toContain('Validation failed');
    });

    it('shows the fixed unavailable message for a 404', () => {
      failWith(httpError(404, { status: 404, message: 'Clínica no encontrada' }));

      expect(service.status()).toBe('unavailable');
      expect(service.message()).toBe(UNAVAILABLE_MESSAGE);
    });

    it('shows the fixed unavailable message for a 404 without body', () => {
      failWith(httpError(404));

      expect(service.status()).toBe('unavailable');
      expect(service.message()).toBe(UNAVAILABLE_MESSAGE);
    });

    it('does not store a token when the clinic is unavailable', () => {
      failWith(httpError(400));

      expect(sessionStorage.getItem(GUEST_TOKEN_KEY)).toBeNull();
      expect(service.ready()).toBe(false);
    });

    it('reports a 500 without body as an error with the fallback message', () => {
      failWith(httpError(500));

      expect(service.status()).toBe('error');
      expect(service.message()).toBe(FALLBACK_MESSAGE);
    });

    it('reports a 500 with a backend message as an error with that message', () => {
      failWith(httpError(500, { status: 500, message: 'Error interno' }));

      expect(service.status()).toBe('error');
      expect(service.message()).toBe('Error interno');
    });

    it('reports a network failure as a connection error', () => {
      failWith(httpError(0));

      expect(service.status()).toBe('error');
      expect(service.message()).toBe('No se pudo conectar con el servidor');
    });

    it('reports a non HTTP error with the fallback message', () => {
      failWith(new Error('boom'));

      expect(service.status()).toBe('error');
      expect(service.message()).toBe(FALLBACK_MESSAGE);
    });

    describe('rate limiting (429)', () => {
      beforeEach(() => {
        vi.useFakeTimers();
      });

      function rateLimited(retryAfter = '30') {
        return httpError(429, null, new HttpHeaders({ 'Retry-After': retryAfter }));
      }

      it('reports an error with the rate limit message and starts the cooldown', () => {
        failWith(rateLimited());

        expect(service.status()).toBe('error');
        expect(service.message()).toBe(RATE_LIMIT_MESSAGE);
        expect(service.coolingDown()).toBe(true);
        expect(service.retryInLabel()).toBe('30 s');
      });

      it('ignores retry while cooling down', () => {
        failWith(rateLimited());

        service.retry();

        expect(guestApi.createSession).toHaveBeenCalledOnce();
      });

      it('allows retrying once the cooldown finishes', () => {
        failWith(rateLimited());

        vi.advanceTimersByTime(30_000);
        expect(service.coolingDown()).toBe(false);
        expect(service.cooldownFinished()).toBe(true);

        guestApi.createSession.mockReturnValue(of(sessionResponse()));
        service.retry();

        expect(guestApi.createSession).toHaveBeenCalledTimes(2);
        expect(service.status()).toBe('ready');
      });

      it('does not start a cooldown without a Retry-After header', () => {
        failWith(httpError(429));

        expect(service.status()).toBe('error');
        expect(service.message()).toBe(RATE_LIMIT_MESSAGE);
        expect(service.coolingDown()).toBe(false);
      });

      it('keeps a ready session when a later request is rate limited', () => {
        service.start(SLUG);

        service.fail(rateLimited());

        expect(service.status()).toBe('ready');
        expect(service.message()).toBeNull();
        expect(service.coolingDown()).toBe(false);
      });
    });
  });
});
