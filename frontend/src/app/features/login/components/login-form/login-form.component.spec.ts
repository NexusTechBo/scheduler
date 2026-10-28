import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LoginService } from '../../login.service';
import { LoginFormComponent } from './login-form.component';

describe('LoginFormComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginFormComponent],
      providers: [LoginService, provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('prefills the username from the navigation state', () => {
    spyOn(TestBed.inject(Location), 'getState').and.returnValue({ username: 'ana@central' });
    const fixture = TestBed.createComponent(LoginFormComponent);
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('#login-username') as HTMLInputElement;
    expect(input.value).toBe('ana@central');
  });

  it('renders the pending-activation box with the resend form on 403', () => {
    const fixture = TestBed.createComponent(LoginFormComponent);
    fixture.detectChanges();

    TestBed.inject(LoginService).login('ana@central', 'secret123');
    http.expectOne('auth/login').flush(null, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain(
      'Tu clínica aún no está activada. Revisa tu correo para activarla.',
    );
    expect(element.querySelector('#login-resend-email')).toBeTruthy();
  });
});
