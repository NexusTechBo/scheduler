import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { ClinicRequest } from '../../core/models/api.models';
import { NotificationService } from '../../shared/services/notification.service';
import { RegisterService } from './register.service';

const REQUEST: ClinicRequest = {
  name: 'Clínica Central',
  subdomain: 'central',
  phoneNumber: '70000000',
  adminName: 'Ana Pérez',
  adminUsername: 'ana',
  adminEmail: 'ana@central.com',
  adminPassword: 'secret123',
  adminCi: '1234567',
};

describe('RegisterService', () => {
  let service: RegisterService;
  let http: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;
  let notifications: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    notifications = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'success',
      'error',
    ]);
    TestBed.configureTestingModule({
      providers: [
        RegisterService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    service = TestBed.inject(RegisterService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows the check-your-email state after creating the clinic, without logging in', () => {
    spyOn(localStorage, 'setItem').and.callThrough();

    service.register(REQUEST);
    expect(service.submitting()).toBeTrue();

    const req = http.expectOne('clinics');
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 1,
      name: REQUEST.name,
      phoneNumber: REQUEST.phoneNumber,
      subdomain: REQUEST.subdomain,
      adminUsername: 'ana@central.com',
      adminEmail: REQUEST.adminEmail,
    });

    expect(service.registeredEmail()).toBe(REQUEST.adminEmail);
    expect(service.submitting()).toBeFalse();
    expect(auth.login).not.toHaveBeenCalled();
    expect(localStorage.setItem).not.toHaveBeenCalled();
    http.expectNone('auth/login');
  });

  it('ignores a second submit while the first one is in flight', () => {
    service.register(REQUEST);
    service.register(REQUEST);
    http.expectOne('clinics').flush(null, { status: 500, statusText: 'Error' });
    expect(service.submitting()).toBeFalse();
  });

  it('keeps the form and notifies the error when creation fails', () => {
    service.register(REQUEST);
    http
      .expectOne('clinics')
      .flush(
        { status: 409, message: 'Subdominio en uso' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(service.registeredEmail()).toBeNull();
    expect(notifications.error).toHaveBeenCalledWith('Subdominio en uso');
  });
});
