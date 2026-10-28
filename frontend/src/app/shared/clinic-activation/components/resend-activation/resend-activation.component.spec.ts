import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResendActivationComponent } from './resend-activation.component';

describe('ResendActivationComponent', () => {
  let fixture: ComponentFixture<ResendActivationComponent>;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ResendActivationComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ResendActivationComponent);
  });

  afterEach(() => http.verify());

  const element = (): HTMLElement => fixture.nativeElement;
  const button = (): HTMLButtonElement => element().querySelector('button')!;

  it('blocks the request when the email is invalid', () => {
    fixture.detectChanges();
    const input = element().querySelector<HTMLInputElement>('input[type="email"]')!;
    input.value = 'not-an-email';
    input.dispatchEvent(new Event('input'));

    button().click();
    fixture.detectChanges();

    http.expectNone('clinics/activation/resend');
    expect(element().textContent).toContain('Ingresa un email válido');
  });

  it('hides the field and sends the fixed email', () => {
    fixture.componentRef.setInput('email', 'ana@central.com');
    fixture.detectChanges();

    expect(element().querySelector('input')).toBeNull();
    button().click();

    expect(http.expectOne('clinics/activation/resend').request.body).toEqual({
      email: 'ana@central.com',
    });
  });

  it('starts blocked with a countdown when startCooldown is set', () => {
    fixture.componentRef.setInput('email', 'ana@central.com');
    fixture.componentRef.setInput('startCooldown', true);
    fixture.detectChanges();

    expect(button().disabled).toBeTrue();
    expect(button().textContent).toContain('Reenviar en 60 s');
    fixture.destroy();
  });
});
