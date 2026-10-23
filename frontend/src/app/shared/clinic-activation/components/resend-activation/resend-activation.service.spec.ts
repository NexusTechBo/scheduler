import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { RESEND_COOLDOWN_SECONDS, ResendActivationService } from './resend-activation.service';

describe('ResendActivationService', () => {
  let service: ResendActivationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ResendActivationService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ResendActivationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('confirms and blocks new resends during the cooldown', fakeAsync(() => {
    service.resend('ana@central.com');
    const req = http.expectOne('clinics/activation/resend');
    expect(req.request.body).toEqual({ email: 'ana@central.com' });
    req.flush(null, { status: 202, statusText: 'Accepted' });

    expect(service.sent()).toBeTrue();
    expect(service.remaining()).toBe(RESEND_COOLDOWN_SECONDS);
    expect(service.canSend()).toBeFalse();

    service.resend('ana@central.com');
    http.expectNone('clinics/activation/resend');

    tick(15_000);
    expect(service.remaining()).toBe(RESEND_COOLDOWN_SECONDS - 15);

    tick((RESEND_COOLDOWN_SECONDS - 15) * 1000);
    expect(service.remaining()).toBe(0);
    expect(service.canSend()).toBeTrue();
    discardPeriodicTasks();
  }));

  it('shows a Spanish message for an invalid email', () => {
    service.resend('nope');
    http
      .expectOne('clinics/activation/resend')
      .flush(
        { status: 400, message: 'Validation failed', errors: [{ field: 'email', message: 'x' }] },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(service.error()).toBe('Ingresa un email válido');
    expect(service.sent()).toBeFalse();
    expect(service.canSend()).toBeTrue();
  });

  it('uses a neutral message for a 400 when the email is fixed', () => {
    service.resend('ana@central.com', false);
    http
      .expectOne('clinics/activation/resend')
      .flush(null, { status: 400, statusText: 'Bad Request' });

    expect(service.error()).toBe('No se pudo reenviar el correo, intenta de nuevo');
  });
});
