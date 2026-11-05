import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { Patient } from '../../../core/models/api.models';
import { PatientFormData, PatientFormDialogComponent } from './patient-form-dialog.component';
import { PatientFormService } from './patient-form.service';

const CI_MESSAGE = 'Solo números, de 5 a 10 dígitos';

function patient(overrides: Partial<Patient> = {}): Patient {
  return {
    id: 7,
    ci: '1234567',
    name: 'Ana Rojas',
    email: null,
    phoneNumber: '70000000',
    active: true,
    ...overrides,
  };
}

describe('PatientFormDialogComponent', () => {
  let fixture: ComponentFixture<PatientFormDialogComponent>;
  let element: HTMLElement;
  let patients: { save: ReturnType<typeof vi.fn> };
  let dialogRef: { id: string; close: ReturnType<typeof vi.fn> };

  async function setup(data: PatientFormData = {}): Promise<void> {
    patients = { save: vi.fn((_: unknown, request: object) => of(patient(request))) };
    dialogRef = { id: 'patient-dialog', close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [PatientFormDialogComponent],
      providers: [
        provideZoneChangeDetection(),
        { provide: PatientFormService, useValue: patients },
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(PatientFormDialogComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  function field(label: string): HTMLInputElement {
    const labelElement = [...element.querySelectorAll('label')].find(
      (l) => l.textContent?.trim() === label,
    );
    return element.querySelector<HTMLInputElement>(`[id="${labelElement!.htmlFor}"]`)!;
  }

  function submitButton(): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  function ciError(): string | undefined {
    return element.querySelector('#patient-ci-error')?.textContent?.trim();
  }

  async function type(label: string, value: string): Promise<void> {
    const input = field(label);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function fillValid(ci = '1234567'): Promise<void> {
    await type('Nombre completo', '  Ana Rojas  ');
    await type('Teléfono', '70000000');
    await type('CI', ci);
  }

  async function submit(): Promise<void> {
    submitButton().click();
    await fixture.whenStable();
  }

  describe('create mode', () => {
    beforeEach(() => setup());

    it('renders the create title and an empty CI limited to 10 characters', () => {
      expect(element.querySelector('h2')?.textContent).toContain('Nuevo paciente');
      expect(field('CI').value).toBe('');
      expect(field('CI').getAttribute('maxlength')).toBe('10');
      expect(submitButton().textContent).toContain('Crear paciente');
    });

    it('creates the patient with trimmed values and closes with the saved patient', async () => {
      await fillValid('1234567');

      await submit();

      expect(patients.save).toHaveBeenCalledWith(undefined, {
        name: 'Ana Rojas',
        ci: '1234567',
        phoneNumber: '70000000',
        email: null,
      });
      expect(dialogRef.close).toHaveBeenCalledWith(expect.objectContaining({ ci: '1234567' }));
    });

    it('asks for the required fields on an empty submit', async () => {
      await submit();

      expect(ciError()).toBe('Este campo es obligatorio');
      expect(patients.save).not.toHaveBeenCalled();
    });

    describe('CI validation (DEF-02)', () => {
      it.each([
        ['1234', 'too short'],
        ['12a45', 'with letters'],
        ['12 345', 'with spaces'],
      ])('shows the CI rule and blocks submit for "%s" (%s)', async (ci) => {
        await fillValid(ci);

        expect(ciError()).toBe(CI_MESSAGE);
        expect(field('CI').getAttribute('aria-invalid')).toBe('true');
        await submit();
        expect(patients.save).not.toHaveBeenCalled();
        expect(dialogRef.close).not.toHaveBeenCalled();
      });

      it('shows the CI rule, not the length message, for a pasted value over 10 digits', async () => {
        await fillValid('12345678901');

        expect(ciError()).toBe(CI_MESSAGE);
        await submit();
        expect(patients.save).not.toHaveBeenCalled();
      });

      it.each(['12345', '1234567890'])('accepts the boundary value "%s"', async (ci) => {
        await fillValid(ci);

        expect(ciError()).toBeUndefined();
        await submit();
        expect(patients.save).toHaveBeenCalledWith(undefined, expect.objectContaining({ ci }));
      });
    });

    it('disables the submit button and ignores double submit while saving', async () => {
      patients.save.mockReturnValue(new Subject<Patient>());
      await fillValid();

      await submit();
      expect(submitButton().disabled).toBe(true);
      expect(submitButton().textContent).toContain('Guardando…');

      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      expect(patients.save).toHaveBeenCalledOnce();
    });

    it('shows the API error and stays open on failure', async () => {
      patients.save.mockReturnValue(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 409,
              error: { status: 409, message: 'Ya existe un paciente con ese CI' },
            }),
        ),
      );
      await fillValid();

      await submit();

      expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'Ya existe un paciente con ese CI',
      );
      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(submitButton().disabled).toBe(false);
    });

    it('closes without saving from the cancel button', () => {
      [...element.querySelectorAll<HTMLButtonElement>('button')]
        .find((b) => b.textContent?.trim() === 'Cancelar')!
        .click();

      expect(dialogRef.close).toHaveBeenCalledWith();
      expect(patients.save).not.toHaveBeenCalled();
    });
  });

  describe('edit mode', () => {
    it('prefills the form and updates the patient', async () => {
      const existing = patient();
      await setup({ patient: existing });

      expect(element.querySelector('h2')?.textContent).toContain('Editar paciente');
      expect(field('CI').value).toBe('1234567');
      await submit();

      expect(patients.save).toHaveBeenCalledWith(existing, {
        name: 'Ana Rojas',
        ci: '1234567',
        phoneNumber: '70000000',
        email: null,
      });
    });

    it('blocks saving an existing patient whose CI no longer matches the rule', async () => {
      await setup({ patient: patient({ ci: '1234567-LP' }) });

      await submit();

      expect(ciError()).toBe(CI_MESSAGE);
      expect(patients.save).not.toHaveBeenCalled();
    });
  });
});
