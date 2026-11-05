import { Dialog } from '@angular/cdk/dialog';
import { computed, provideZoneChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EMPTY, of } from 'rxjs';
import { Patient } from '../../core/models/api.models';
import { PatientFormDialogComponent } from '../../shared/components/patient-form-dialog/patient-form-dialog.component';
import { PatientsPageComponent } from './patients-page.component';
import { PatientScope } from './patients.model';
import { PatientsService } from './patients.service';

function patient(overrides: Partial<Patient> = {}): Patient {
  return {
    id: 20,
    ci: '1234567',
    name: 'Ana Pérez',
    email: 'ana@mail.test',
    phoneNumber: '70000000',
    active: true,
    ...overrides,
  };
}

function createPatientsStub() {
  const patients = signal<Patient[]>([]);
  return {
    ownListEnabled: false,
    canManageDoctors: false,
    scope: signal<PatientScope>('all'),
    search: signal(''),
    showInactive: signal(false),
    isLoading: signal(false),
    errorMessage: signal<string | null>(null),
    patients,
    rows: computed(() => patients().map((p) => ({ patient: p, mine: false }))),
    pagination: signal({ number: 0, totalElements: 0, totalPages: 0 }),
    reload: vi.fn(),
    reloadMine: vi.fn(),
    setScope: vi.fn(),
    setSearch: vi.fn(),
    setShowInactive: vi.fn(),
    setPage: vi.fn(),
    deactivate: vi.fn(() => EMPTY),
    activate: vi.fn(() => EMPTY),
    toggleMine: vi.fn(() => EMPTY),
  };
}

describe('PatientsPageComponent', () => {
  let fixture: ComponentFixture<PatientsPageComponent>;
  let element: HTMLElement;
  let patients: ReturnType<typeof createPatientsStub>;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    patients = createPatientsStub();
    dialog = { open: vi.fn(() => ({ closed: EMPTY })) };
    TestBed.configureTestingModule({
      imports: [PatientsPageComponent],
      providers: [provideZoneChangeDetection(), { provide: Dialog, useValue: dialog }],
    });
    TestBed.overrideComponent(PatientsPageComponent, {
      set: { providers: [{ provide: PatientsService, useValue: patients }] },
    });
    fixture = TestBed.createComponent(PatientsPageComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  async function render(): Promise<void> {
    await fixture.whenStable();
  }

  function buttonByLabel(label: string): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>(`button[aria-label="${label}"]`)!;
  }

  function buttonByText(text: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(text),
    )!;
  }

  describe('states', () => {
    it('shows a skeleton while loading without data', async () => {
      patients.isLoading.set(true);
      await render();
      expect(element.querySelector('app-skeleton')).not.toBeNull();
      expect(element.querySelector('table')).toBeNull();
    });

    it('keeps the table while reloading with data', async () => {
      patients.patients.set([patient()]);
      patients.isLoading.set(true);
      await render();
      expect(element.querySelector('app-skeleton')).toBeNull();
      expect(element.querySelector('table')).not.toBeNull();
    });

    it('shows the load error and retries', async () => {
      patients.errorMessage.set('No se pudieron cargar los pacientes');
      await render();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain(
        'No se pudieron cargar los pacientes',
      );
      element.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
      expect(patients.reload).toHaveBeenCalled();
    });

    it('shows the empty state for a clinic without patients', async () => {
      await render();
      expect(element.textContent).toContain('No hay pacientes');
      expect(element.textContent).toContain('Registra al primer paciente de la clínica.');
    });

    it('shows the no results state while searching', async () => {
      patients.search.set('zzz');
      await render();
      expect(element.textContent).toContain('Sin resultados');
      expect(element.textContent).toContain('No hay pacientes que coincidan con la búsqueda.');
    });
  });

  describe('table', () => {
    beforeEach(async () => {
      patients.patients.set([
        patient(),
        patient({ id: 21, ci: '7654321', name: 'Luis Rojas', email: null, active: false }),
      ]);
      await render();
    });

    it('renders the CI column header between patient and phone', () => {
      const headers = [...element.querySelectorAll('thead th')].map((th) => th.textContent?.trim());
      expect(headers).toEqual(['Paciente', 'CI', 'Teléfono', 'Estado', 'Acciones']);
    });

    it('renders the CI in its own cell for each patient', () => {
      const rows = element.querySelectorAll('tbody tr');
      expect(rows).toHaveLength(2);
      const cells = [...rows[0].querySelectorAll('td')].map((td) => td.textContent?.trim());
      expect(cells[1]).toBe('1234567');
      expect(cells[2]).toBe('70000000');
      expect(rows[1].querySelectorAll('td')[1].textContent?.trim()).toBe('7654321');
    });

    it('shows the CI under the name for small screens', () => {
      const firstCell = element.querySelector('tbody tr td')!;
      expect(firstCell.textContent).toContain('Ana Pérez');
      expect(firstCell.textContent).toContain('CI 1234567');
      expect(firstCell.textContent).toContain('ana@mail.test');
    });

    it('shows the status of each patient', () => {
      const rows = element.querySelectorAll('tbody tr');
      expect(rows[0].textContent).toContain('Activo');
      expect(rows[1].textContent).toContain('Inactivo');
    });

    it('offers deactivate for active patients and reactivate for inactive ones', () => {
      expect(buttonByLabel('Desactivar a Ana Pérez')).not.toBeNull();
      expect(buttonByLabel('Reactivar a Luis Rojas')).not.toBeNull();
      expect(buttonByLabel('Reactivar a Ana Pérez')).toBeNull();
    });

    it('deactivates a patient through the service', () => {
      buttonByLabel('Desactivar a Ana Pérez').click();
      expect(patients.deactivate).toHaveBeenCalledWith(patient());
    });

    it('reactivates a patient through the service', () => {
      buttonByLabel('Reactivar a Luis Rojas').click();
      expect(patients.activate).toHaveBeenCalledWith(expect.objectContaining({ id: 21 }));
    });

    it('hides assignment actions when the feature is disabled', () => {
      expect(element.textContent).not.toContain('Asignarme');
      expect(element.querySelector('button[aria-label^="Doctores asignados"]')).toBeNull();
    });
  });

  describe('filters', () => {
    it('sends the search text to the service', async () => {
      const input = element.querySelector<HTMLInputElement>(
        'input[aria-label="Buscar pacientes"]',
      )!;
      input.value = 'ana';
      input.dispatchEvent(new Event('input'));
      await render();
      expect(patients.setSearch).toHaveBeenCalledWith('ana');
    });

    it('toggles inactive patients through the service', async () => {
      element.querySelector<HTMLInputElement>('input[type="checkbox"]')!.click();
      await render();
      expect(patients.setShowInactive).toHaveBeenCalledWith(true);
    });

    it('changes the page through the service', async () => {
      patients.patients.set([patient()]);
      patients.pagination.set({ number: 0, totalElements: 30, totalPages: 3 });
      await render();

      buttonByLabel('Página siguiente').click();

      expect(patients.setPage).toHaveBeenCalledWith(1);
    });
  });

  describe('patient form', () => {
    it('opens the patient form and reloads when a patient is saved', () => {
      dialog.open.mockReturnValue({ closed: of(patient()) });

      buttonByText('Nuevo paciente').click();

      expect(dialog.open).toHaveBeenCalledWith(
        PatientFormDialogComponent,
        expect.objectContaining({ data: { patient: undefined } }),
      );
      expect(patients.reload).toHaveBeenCalledOnce();
    });

    it('opens the form with the patient to edit', async () => {
      patients.patients.set([patient()]);
      await render();

      buttonByLabel('Editar a Ana Pérez').click();

      expect(dialog.open).toHaveBeenCalledWith(
        PatientFormDialogComponent,
        expect.objectContaining({ data: { patient: patient() } }),
      );
    });

    it('does not reload when the form is dismissed', () => {
      dialog.open.mockReturnValue({ closed: of(undefined) });
      buttonByText('Nuevo paciente').click();
      expect(patients.reload).not.toHaveBeenCalled();
    });
  });
});
