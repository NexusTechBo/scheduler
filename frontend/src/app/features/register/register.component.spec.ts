import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegisterComponent } from './register.component';
import { RegisterService } from './register.service';

describe('RegisterComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('switches to the success view with the resend button already counting down', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('app-register-form')).toBeTruthy();

    fixture.debugElement.injector.get(RegisterService).register({
      name: 'Clínica Central',
      slug: 'central',
      phoneNumber: '70000000',
      adminName: 'Ana Pérez',
      adminUsername: 'ana',
      adminEmail: 'ana@central.com',
      adminPassword: 'secret123',
      adminCi: '1234567',
    });
    http.expectOne('clinics').flush({
      id: 1,
      name: 'Clínica Central',
      phoneNumber: '70000000',
      slug: 'central',
      adminUsername: 'ana@central.com',
      adminEmail: 'ana@central.com',
    });
    fixture.detectChanges();

    expect(element.querySelector('app-register-form')).toBeNull();
    expect(element.textContent).toContain('¡Clínica registrada!');
    expect(element.textContent).toContain('ana@central.com');

    const resend = element.querySelector<HTMLButtonElement>('app-resend-activation button')!;
    expect(resend.disabled).toBeTrue();
    expect(resend.textContent).toContain('Reenviar en 60 s');
    fixture.destroy();
  });
});
