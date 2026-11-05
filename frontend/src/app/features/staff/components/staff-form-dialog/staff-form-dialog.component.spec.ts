import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { WritableSignal, provideZoneChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { Specialty, Staff } from '../../../../core/models/api.models';
import { StaffService } from '../../staff.service';
import { StaffFormData, StaffFormDialogComponent } from './staff-form-dialog.component';

const CI_MESSAGE = 'Solo números, de 5 a 10 dígitos';

function staff(overrides: Partial<Staff> = {}): Staff {
  return {
    id: 3,
    username: 'luis.perez@clinicacentral.com',
    ci: '1234567-LP',
    name: 'Luis Pérez',
    email: 'luis@email.com',
    phoneNumber: null,
    active: true,
    alsoDoctor: false,
    role: 'ASSISTANT',
    specialtyId: null,
    specialtyName: null,
    ...overrides,
  };
}

describe('StaffFormDialogComponent', () => {
  let fixture: ComponentFixture<StaffFormDialogComponent>;
  let element: HTMLElement;
  let staffService: {
    specialties: WritableSignal<Specialty[]>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let dialogRef: { id: string; close: ReturnType<typeof vi.fn> };

  async function setup(data: StaffFormData = {}): Promise<void> {
    staffService = {
      specialties: signal<Specialty[]>([{ id: 1, name: 'Cardiología' }]),
      create: vi.fn(() => of(staff())),
      update: vi.fn(() => of(staff())),
    };
    dialogRef = { id: 'staff-dialog', close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [StaffFormDialogComponent],
      providers: [
        provideZoneChangeDetection(),
        { provide: StaffService, useValue: staffService },
        {
          provide: AuthService,
          useValue: { clinic: signal({ name: 'Clínica Central', slug: 'clinicacentral' }) },
        },
        { provide: DialogRef, useValue: dialogRef },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(StaffFormDialogComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  }

  function field(label: string): HTMLInputElement | null {
    const labelElement = [...element.querySelectorAll('label')].find(
      (l) => l.textContent?.trim() === label,
    );
    return labelElement
      ? element.querySelector<HTMLInputElement>(`[id="${labelElement.htmlFor}"]`)
      : null;
  }

  async function type(label: string, value: string): Promise<void> {
    const input = field(label)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function chooseRole(label: string): Promise<void> {
    [...element.querySelectorAll('label')]
      .find((l) => l.textContent?.trim() === label)!
      .querySelector<HTMLInputElement>('input[type="radio"]')!
      .click();
    await fixture.whenStable();
  }

  async function submit(): Promise<void> {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  function ciError(): string | undefined {
    return element.querySelector('#staff-ci-error')?.textContent?.trim();
  }

  describe('create mode', () => {
    beforeEach(async () => {
      await setup();
      await chooseRole('Asistente');
      await type('Nombre completo', 'Luis Pérez');
      await type('Email', 'luis@email.com');
      await type('Usuario', 'luis.perez');
      await type('Contraseña inicial', 'secreto123');
    });

    it('renders the CI field limited to 10 characters', () => {
      expect(field('CI')?.getAttribute('maxlength')).toBe('10');
    });

    it.each(['1234', '12a45', '12 345', '12345678901'])(
      'shows the CI rule and blocks submit for "%s" (DEF-02)',
      async (ci) => {
        await type('CI', ci);

        expect(ciError()).toBe(CI_MESSAGE);
        await submit();
        expect(staffService.create).not.toHaveBeenCalled();
        expect(dialogRef.close).not.toHaveBeenCalled();
      },
    );

    it('creates the member with a valid CI and closes', async () => {
      await type('CI', '1234567890');

      await submit();

      expect(ciError()).toBeUndefined();
      expect(staffService.create).toHaveBeenCalledWith(
        expect.objectContaining({ ci: '1234567890', role: 'ASSISTANT', username: 'luis.perez' }),
      );
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });
  });

  describe('edit mode', () => {
    it('does not render nor validate the CI and saves without it', async () => {
      await setup({ staff: staff({ ci: '1234567-LP' }) });

      expect(field('CI')).toBeNull();
      expect(element.querySelector('#staff-ci')).toBeNull();
      await submit();

      expect(ciError()).toBeUndefined();
      expect(staffService.update).toHaveBeenCalledWith(3, {
        name: 'Luis Pérez',
        email: 'luis@email.com',
        phoneNumber: null,
        role: 'ASSISTANT',
        specialtyId: null,
        password: null,
      });
      expect(staffService.update.mock.calls[0][1]).not.toHaveProperty('ci');
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });
  });
});
