import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ActivationComponent } from './activation.component';

describe('ActivationComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'activar', component: ActivationComponent },
          { path: 'login', children: [] },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function open(url: string): Promise<{ harness: RouterTestingHarness; text: () => string }> {
    const harness = await RouterTestingHarness.create(url);
    harness.detectChanges();
    await harness.fixture.whenStable();
    return { harness, text: () => harness.routeNativeElement?.textContent ?? '' };
  }

  it('activates once, removes the token from the URL and shows the username', async () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.callThrough();
    const { harness, text } = await open('/activar?token=abc123');

    expect(navigate).toHaveBeenCalledWith(
      [],
      jasmine.objectContaining({ queryParams: {}, replaceUrl: true }),
    );
    expect(router.url).toBe('/activar');
    expect(text()).toContain('Activando tu clínica…');

    const req = http.expectOne('clinics/activate');
    expect(req.request.body).toEqual({ token: 'abc123' });
    req.flush({ clinicName: 'Clínica Central', adminUsername: 'ana@central' });
    harness.detectChanges();

    expect(text()).toContain('¡Tu clínica Clínica Central está activa!');
    expect(text()).toContain('ana@central');
    expect(text()).toContain('Iniciar sesión');
    expect(document.activeElement?.id).toBe('activation-title');

    harness.detectChanges();
    http.expectNone('clinics/activate');
  });

  it('with an open session, logs out and goes to login with the new username', async () => {
    const encode = (value: object): string =>
      btoa(JSON.stringify(value)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
    const token = [
      encode({ alg: 'HS256' }),
      encode({
        sub: '7',
        role: 'ADMINISTRATOR',
        clinicId: 1,
        clinicName: 'Otra',
        clinicSubdomain: 'otra',
        username: 'otro@otra',
        exp: Math.floor(Date.now() / 1000) + 3600,
      }),
      'signature',
    ].join('.');
    localStorage.setItem('scheduler.token', token);

    try {
      const router = TestBed.inject(Router);
      // No whenStable(): the session's profile request (auth/me) keeps the app unstable.
      const harness = await RouterTestingHarness.create('/activar?token=abc123');
      harness.detectChanges();
      const text = (): string => harness.routeNativeElement?.textContent ?? '';
      http.match('auth/me').forEach((req) => req.flush(null));
      http
        .expectOne('clinics/activate')
        .flush({ clinicName: 'Clínica Central', adminUsername: 'ana@central' });
      harness.detectChanges();

      expect(text()).toContain('Tienes una sesión abierta.');
      const navigate = spyOn(router, 'navigate').and.callThrough();
      const button = Array.from(
        harness.routeNativeElement?.querySelectorAll<HTMLButtonElement>('button') ?? [],
      ).find((b) => b.textContent?.includes('Iniciar sesión'));
      button?.click();

      expect(localStorage.getItem('scheduler.token')).toBeNull();
      expect(navigate).toHaveBeenCalledWith(['/login'], { state: { username: 'ana@central' } });
    } finally {
      localStorage.removeItem('scheduler.token');
    }
  });

  it('shows the invalid-link state and the resend form on 400', async () => {
    const { harness, text } = await open('/activar?token=used');

    http
      .expectOne('clinics/activate')
      .flush(
        { status: 400, message: 'The activation link is invalid or has expired' },
        { status: 400, statusText: 'Bad Request' },
      );
    harness.detectChanges();

    expect(text()).toContain('El enlace de activación no es válido o ya venció.');
    expect(harness.routeNativeElement?.querySelector('#activation-resend-email')).toBeTruthy();
  });

  it('shows the invalid-link state without calling the API when there is no token', async () => {
    const { text } = await open('/activar');

    http.expectNone('clinics/activate');
    expect(text()).toContain('El enlace de activación no es válido o ya venció.');
  });

  it('shows a retryable error on network failure', async () => {
    const { harness, text } = await open('/activar?token=abc123');

    http.expectOne('clinics/activate').error(new ProgressEvent('error'), { status: 0 });
    harness.detectChanges();

    expect(text()).toContain('No se pudo conectar con el servidor');
    const retry = harness.routeNativeElement?.querySelector<HTMLButtonElement>('button');
    retry?.click();
    harness.detectChanges();

    const req = http.expectOne('clinics/activate');
    expect(req.request.body).toEqual({ token: 'abc123' });
    req.flush(null, { status: 400, statusText: 'Bad Request' });
    harness.detectChanges();
    expect(document.activeElement?.id).toBe('activation-title');
  });
});
