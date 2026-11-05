import { provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuestBookingPatient } from '../../../../core/models/api.models';
import { GuestPatientFormComponent } from './guest-patient-form.component';

const CI_MESSAGE = 'Solo números, de 5 a 10 dígitos';

describe('GuestPatientFormComponent', () => {
  let fixture: ComponentFixture<GuestPatientFormComponent>;
  let element: HTMLElement;
  let submitted: GuestBookingPatient[];

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [GuestPatientFormComponent],
      providers: [provideZoneChangeDetection()],
    });
    fixture = TestBed.createComponent(GuestPatientFormComponent);
    submitted = [];
    fixture.componentInstance.submitted.subscribe((patient) => submitted.push(patient));
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

  async function chooseKind(label: string): Promise<void> {
    [...element.querySelectorAll('label')]
      .find((l) => l.textContent?.trim() === label)!
      .querySelector<HTMLInputElement>('input[type="radio"]')!
      .click();
    await fixture.whenStable();
  }

  async function type(label: string, value: string): Promise<void> {
    const input = field(label);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function submit(): Promise<void> {
    [...element.querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.trim() === 'Continuar')!
      .click();
    await fixture.whenStable();
  }

  function ciError(): string | undefined {
    return element.querySelector('#guest-ci-error')?.textContent?.trim();
  }

  describe('CI validation (DEF-02)', () => {
    beforeEach(async () => {
      await chooseKind('Ya me atendí en esta clínica');
      await type('Teléfono', '70000000');
    });

    it('limits the CI input to 10 characters', () => {
      expect(field('CI').getAttribute('maxlength')).toBe('10');
    });

    it.each(['1234', '12a45', '12 345', '12345678901'])(
      'shows the CI rule and does not emit for "%s"',
      async (ci) => {
        await type('CI', ci);

        expect(ciError()).toBe(CI_MESSAGE);
        await submit();
        expect(submitted).toEqual([]);
      },
    );

    it('emits a returning patient with a valid CI', async () => {
      await type('CI', '12345');

      await submit();

      expect(ciError()).toBeUndefined();
      expect(submitted).toEqual([{ ci: '12345', phoneNumber: '70000000' }]);
    });
  });
});
