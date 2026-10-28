import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivationService } from './activation.service';

describe('ActivationService', () => {
  let service: ActivationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ActivationService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ActivationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the token exactly once even if activate() is called again', () => {
    service.activate('abc123');
    service.activate('abc123');
    service.activate('other');

    http
      .expectOne('clinics/activate')
      .flush({ clinicName: 'Clínica Central', adminUsername: 'ana@central' });
    expect(service.status()).toBe('success');

    service.activate('abc123');
    http.expectNone('clinics/activate');
  });

  it('does not allow retrying a consumed or invalid token', () => {
    service.activate('used');
    http.expectOne('clinics/activate').flush(null, { status: 400, statusText: 'Bad Request' });
    expect(service.status()).toBe('invalid');

    service.retry();
    http.expectNone('clinics/activate');
  });
});
