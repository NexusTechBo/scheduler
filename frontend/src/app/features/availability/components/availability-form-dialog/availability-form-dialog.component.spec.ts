import { DialogRef } from '@angular/cdk/dialog';
import { provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Subject, of } from 'rxjs';
import { DayOfWeek } from '../../../../core/models/api.models';
import { TimePickerComponent } from '../../../../shared/ui/forms/time-picker/time-picker.component';
import { AddAvailabilityResult } from '../../availability.model';
import { AvailabilityService } from '../../availability.service';
import { AvailabilityFormDialogComponent } from './availability-form-dialog.component';

function result(saved: DayOfWeek[], failed: AddAvailabilityResult['failed'] = []) {
  return { saved, failed };
}

describe('AvailabilityFormDialogComponent', () => {
  let fixture: ComponentFixture<AvailabilityFormDialogComponent>;
  let element: HTMLElement;
  let availability: { addAvailability: ReturnType<typeof vi.fn> };
  let dialogRef: { id: string; close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    availability = { addAvailability: vi.fn(() => of(result(['MONDAY']))) };
    dialogRef = { id: 'availability-dialog', close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AvailabilityFormDialogComponent],
      providers: [
        provideZoneChangeDetection(),
        { provide: AvailabilityService, useValue: availability },
        { provide: DialogRef, useValue: dialogRef },
      ],
    });
    fixture = TestBed.createComponent(AvailabilityFormDialogComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function dayButton(short: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button[aria-pressed]')].find(
      (button) => button.textContent?.trim() === short,
    )!;
  }

  function buttonByText(text: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find(
      (button) => button.textContent?.trim() === text,
    )!;
  }

  function timeControl(index: 0 | 1) {
    return fixture.debugElement
      .queryAll(By.directive(TimePickerComponent))
      [index].injector.get(NgControl).control!;
  }

  async function submit(): Promise<void> {
    buttonByText(availabilityButtonLabel()).click();
    await fixture.whenStable();
  }

  function availabilityButtonLabel(): string {
    return element.querySelector<HTMLButtonElement>('button[type="submit"]')!.textContent!.trim();
  }

  describe('initial state', () => {
    it('renders the title, Monday preselected and the default range', () => {
      expect(element.querySelector('h2')?.textContent).toContain('Agregar horario');
      expect(dayButton('Lun').getAttribute('aria-pressed')).toBe('true');
      expect(dayButton('Mar').getAttribute('aria-pressed')).toBe('false');
      expect(element.querySelector('#av-start')?.textContent).toContain('08:00');
      expect(element.querySelector('#av-end')?.textContent).toContain('12:00');
      expect(element.querySelector('#av-duration')?.textContent).toContain('30 minutos');
    });

    it('labels the time pickers', () => {
      const start = element.querySelector('label[for="av-start"]');
      const end = element.querySelector('label[for="av-end"]');
      expect(start?.textContent).toContain('Desde');
      expect(end?.textContent).toContain('Hasta');
    });
  });

  describe('day selection', () => {
    it('toggles a day on and off', async () => {
      dayButton('Mié').click();
      await fixture.whenStable();
      expect(dayButton('Mié').getAttribute('aria-pressed')).toBe('true');

      dayButton('Mié').click();
      await fixture.whenStable();
      expect(dayButton('Mié').getAttribute('aria-pressed')).toBe('false');
    });

    it('asks for at least one day and does not submit without days', async () => {
      dayButton('Lun').click();
      await fixture.whenStable();
      expect(element.textContent).toContain('Selecciona al menos un día');

      await submit();

      expect(availability.addAvailability).not.toHaveBeenCalled();
    });
  });

  describe('validation', () => {
    it('shows the range error when the start moves after the end', async () => {
      timeControl(0).setValue('13:00');
      await fixture.whenStable();

      expect(element.querySelector('#av-end-error')?.textContent).toContain(
        'La hora de fin debe ser posterior a la de inicio',
      );
      expect(element.querySelector('#av-end')?.getAttribute('aria-invalid')).toBe('true');
      expect(element.querySelector('#av-start')?.getAttribute('aria-describedby')).toBe(
        'av-end-error',
      );
    });

    it('does not submit an invalid range', async () => {
      timeControl(1).setValue('07:00');
      await submit();
      expect(availability.addAvailability).not.toHaveBeenCalled();
      expect(element.querySelector('#av-end-error')).not.toBeNull();
    });

    it('clears the range error once the end is after the start again', async () => {
      timeControl(0).setValue('13:00');
      await fixture.whenStable();
      timeControl(1).setValue('15:00');
      await fixture.whenStable();
      expect(element.querySelector('#av-end-error')).toBeNull();
    });
  });

  describe('submit', () => {
    it('saves the selected days in week order with the block and closes', async () => {
      dayButton('Vie').click();
      dayButton('Mié').click();
      await fixture.whenStable();

      await submit();

      expect(availability.addAvailability).toHaveBeenCalledWith(['MONDAY', 'WEDNESDAY', 'FRIDAY'], {
        startTime: '08:00',
        endTime: '12:00',
        slotDurationMinutes: 30,
      });
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });

    it('disables the submit button and ignores double submit while saving', async () => {
      const pending = new Subject<AddAvailabilityResult>();
      availability.addAvailability.mockReturnValue(pending);

      await submit();
      const button = element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
      expect(button.disabled).toBe(true);
      expect(button.textContent).toContain('Guardando…');

      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      expect(availability.addAvailability).toHaveBeenCalledOnce();

      pending.next(result(['MONDAY']));
      pending.complete();
      await fixture.whenStable();
      expect(button.disabled).toBe(false);
      expect(button.textContent).toContain('Agregar');
    });

    it('shows partial failures, keeps only the failed days and stays open', async () => {
      dayButton('Mar').click();
      await fixture.whenStable();
      availability.addAvailability.mockReturnValue(
        of(result(['MONDAY'], [{ day: 'TUESDAY', message: 'Se superpone con otro bloque' }])),
      );

      await submit();

      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'Se guardó: Lunes. No se pudo guardar: Martes (Se superpone con otro bloque)',
      );
      expect(dayButton('Lun').getAttribute('aria-pressed')).toBe('false');
      expect(dayButton('Mar').getAttribute('aria-pressed')).toBe('true');
    });

    it('omits the saved part when every day failed', async () => {
      availability.addAvailability.mockReturnValue(
        of(result([], [{ day: 'MONDAY', message: 'Error' }])),
      );

      await submit();

      expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'No se pudo guardar: Lunes (Error)',
      );
    });

    it('clears the error when the form changes', async () => {
      availability.addAvailability.mockReturnValue(
        of(result([], [{ day: 'MONDAY', message: 'Error' }])),
      );
      await submit();

      timeControl(1).setValue('12:30');
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')).toBeNull();
    });
  });

  describe('closing', () => {
    it('closes from the cancel button', () => {
      buttonByText('Cancelar').click();
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });

    it('closes from the frame close button', () => {
      element.querySelector<HTMLButtonElement>('button[aria-label="Cerrar"]')!.click();
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });
  });
});
