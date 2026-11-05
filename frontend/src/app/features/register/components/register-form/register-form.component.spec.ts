import { provideZoneChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegisterService } from '../../register.service';
import { RegisterFormComponent } from './register-form.component';

const CI_MESSAGE = 'Solo números, de 5 a 10 dígitos';

describe('RegisterFormComponent', () => {
  let fixture: ComponentFixture<RegisterFormComponent>;
  let element: HTMLElement;
  let register: { register: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    register = { register: vi.fn() };
    TestBed.configureTestingModule({
      imports: [RegisterFormComponent],
      providers: [
        provideZoneChangeDetection(),
        provideRouter([]),
        {
          provide: RegisterService,
          useValue: {
            ...register,
            submitting: signal(false),
            coolingDown: signal(false),
            retryInLabel: signal(''),
            cooldownFinished: signal(false),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(RegisterFormComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function field(label: string): HTMLInputElement {
    const labelElement = [...element.querySelectorAll('label')].find(
      (l) => l.textContent?.trim() === label,
    );
    return element.querySelector<HTMLInputElement>(`[id="${labelElement!.htmlFor}"]`)!;
  }

  async function type(label: string, value: string): Promise<void> {
    const input = field(label);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function fillValid(ci: string): Promise<void> {
    await type('Nombre', 'Clínica Central');
    await type('Teléfono', '70000000');
    await type('Identificador de la clínica', 'clinicacentral');
    await type('Nombre completo', 'Ana Rojas');
    await type('CI', ci);
    await type('Usuario', 'admin');
    await type('Correo electrónico', 'admin@email.com');
    await type('Contraseña', 'secreto123');
  }

  async function submit(): Promise<void> {
    [...element.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.trim() === 'Registrar clínica')!
      .click();
    await fixture.whenStable();
  }

  function ciError(): string | undefined {
    return element.querySelector('#register-admin-ci-error')?.textContent?.trim();
  }

  describe('admin CI validation (DEF-02)', () => {
    it('limits the CI input to 10 characters', () => {
      expect(field('CI').getAttribute('maxlength')).toBe('10');
    });

    it.each(['1234', '12a45', '12 345', '12345678901'])(
      'shows the CI rule and does not register for "%s"',
      async (ci) => {
        await fillValid(ci);

        expect(ciError()).toBe(CI_MESSAGE);
        await submit();
        expect(register.register).not.toHaveBeenCalled();
      },
    );

    it('registers the clinic with a valid CI', async () => {
      await fillValid('1234567890');

      await submit();

      expect(ciError()).toBeUndefined();
      expect(register.register).toHaveBeenCalledWith(
        expect.objectContaining({ adminCi: '1234567890', slug: 'clinicacentral' }),
      );
    });
  });
});
