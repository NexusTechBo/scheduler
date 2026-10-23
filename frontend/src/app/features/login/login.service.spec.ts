import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotificationService } from '../../shared/services/notification.service';
import { LoginService } from './login.service';

describe('LoginService', () => {
  let service: LoginService;
  let http: HttpTestingController;
  let notifications: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'success',
      'error',
    ]);
    TestBed.configureTestingModule({
      providers: [
        LoginService,
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: notifications },
      ],
    });
    service = TestBed.inject(LoginService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('flags a pending clinic on 403 instead of showing the raw message', () => {
    service.login('ana@central', 'secret123');
    http.expectOne('auth/login').flush(
      {
        status: 403,
        message: 'The clinic has not been activated yet. Check your email to activate it',
      },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(service.pendingActivation()).toBeTrue();
    expect(service.submitting()).toBeFalse();
    expect(notifications.error).not.toHaveBeenCalled();
  });

  it('keeps the current behaviour for wrong credentials', () => {
    service.login('ana@central', 'wrong');
    http
      .expectOne('auth/login')
      .flush(
        { status: 401, message: 'Credenciales inválidas' },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(service.pendingActivation()).toBeFalse();
    expect(notifications.error).toHaveBeenCalledWith('Credenciales inválidas');
  });

  it('clears the pending flag on a new attempt', () => {
    service.login('ana@central', 'secret123');
    http.expectOne('auth/login').flush(null, { status: 403, statusText: 'Forbidden' });
    expect(service.pendingActivation()).toBeTrue();

    service.login('ana@central', 'secret123');
    expect(service.pendingActivation()).toBeFalse();
    http.expectOne('auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
  });
});
