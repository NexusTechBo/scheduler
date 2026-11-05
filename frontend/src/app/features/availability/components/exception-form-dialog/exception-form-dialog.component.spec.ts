import { DialogRef } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Subject, of, throwError } from 'rxjs';
import { toIso } from '../../../../core/utils/calendar.util';
import { TimePickerComponent } from '../../../../shared/ui/forms/time-picker/time-picker.component';
import { AvailabilityService } from '../../availability.service';
import { ExceptionFormDialogComponent } from './exception-form-dialog.component';

const TODAY = toIso(new Date());

describe('ExceptionFormDialogComponent', () => {
  let fixture: ComponentFixture<ExceptionFormDialogComponent>;
  let element: HTMLElement;
  let availability: { addException: ReturnType<typeof vi.fn> };
  let dialogRef: { id: string; close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    availability = { addException: vi.fn(() => of(undefined)) };
    dialogRef = { id: 'exception-dialog', close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ExceptionFormDialogComponent],
      providers: [
        provideZoneChangeDetection(),
        { provide: AvailabilityService, useValue: availability },
        { provide: DialogRef, useValue: dialogRef },
      ],
    });
    fixture = TestBed.createComponent(ExceptionFormDialogComponent);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  function fullDayCheckbox(): HTMLInputElement {
    return element.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
  }

  function submitButton(): HTMLButtonElement {
    return element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  function timeControl(index: 0 | 1) {
    return fixture.debugElement
      .queryAll(By.directive(TimePickerComponent))
      [index].injector.get(NgControl).control!;
  }

  async function setPartialDay(): Promise<void> {
    fullDayCheckbox().click();
    await fixture.whenStable();
  }

  async function typeReason(value: string): Promise<void> {
    const input = element.querySelector<HTMLInputElement>('#ex-reason')!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  async function submit(): Promise<void> {
    submitButton().click();
    await fixture.whenStable();
  }

  describe('initial state', () => {
    it('blocks the whole day by default without time pickers', () => {
      expect(element.querySelector('h2')?.textContent).toContain('Bloquear agenda');
      expect(fullDayCheckbox().checked).toBe(true);
      expect(element.querySelector('#ex-start')).toBeNull();
      expect(element.querySelector('#ex-end')).toBeNull();
    });

    it('shows the time pickers for a partial block', async () => {
      await setPartialDay();
      expect(element.querySelector('#ex-start')?.textContent).toContain('09:00');
      expect(element.querySelector('#ex-end')?.textContent).toContain('10:00');
      expect(element.querySelector('#ex-end-error')).toBeNull();
    });
  });

  describe('submit', () => {
    it('saves a full day block without times or reason and closes', async () => {
      await submit();

      expect(availability.addException).toHaveBeenCalledWith({
        date: TODAY,
        isFullDayBlock: true,
        startTime: null,
        endTime: null,
        reason: null,
      });
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });

    it('saves a partial block with its times and a trimmed reason', async () => {
      await setPartialDay();
      await typeReason('  Congreso  ');

      await submit();

      expect(availability.addException).toHaveBeenCalledWith({
        date: TODAY,
        isFullDayBlock: false,
        startTime: '09:00',
        endTime: '10:00',
        reason: 'Congreso',
      });
    });

    it('rejects a partial block whose end is not after the start', async () => {
      await setPartialDay();
      timeControl(0).setValue('11:00');
      await fixture.whenStable();

      expect(element.querySelector('#ex-end-error')?.textContent).toContain(
        'La hora de fin debe ser posterior a la de inicio',
      );
      await submit();
      expect(availability.addException).not.toHaveBeenCalled();
    });

    it('asks for the range when a partial block has no end time', async () => {
      await setPartialDay();
      timeControl(1).setValue('');
      timeControl(1).markAsTouched();
      await fixture.whenStable();

      expect(element.querySelector('#ex-end-error')?.textContent).toContain(
        'Indica el rango de horas',
      );
      await submit();
      expect(availability.addException).not.toHaveBeenCalled();
    });

    it('ignores an invalid range again once the whole day is selected', async () => {
      await setPartialDay();
      timeControl(0).setValue('11:00');
      await fixture.whenStable();

      fullDayCheckbox().click();
      await fixture.whenStable();
      await submit();

      expect(availability.addException).toHaveBeenCalledWith(
        expect.objectContaining({ isFullDayBlock: true, startTime: null, endTime: null }),
      );
    });

    it('disables the submit button and ignores double submit while saving', async () => {
      availability.addException.mockReturnValue(new Subject<void>());

      await submit();
      expect(submitButton().disabled).toBe(true);
      expect(submitButton().textContent).toContain('Guardando…');

      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      expect(availability.addException).toHaveBeenCalledOnce();
    });

    it('shows the API error, stays open and focuses the submit button', async () => {
      availability.addException.mockReturnValue(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 409,
              error: { message: 'Ya existe un bloqueo en ese horario' },
            }),
        ),
      );

      await submit();

      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'Ya existe un bloqueo en ese horario',
      );
      expect(submitButton().disabled).toBe(false);
      expect(document.activeElement).toBe(submitButton());
    });

    it('uses the fallback message for unknown errors', async () => {
      availability.addException.mockReturnValue(throwError(() => new Error('boom')));
      await submit();
      expect(element.querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'No se pudo guardar el bloqueo',
      );
    });

    it('clears the error when the form changes', async () => {
      availability.addException.mockReturnValue(throwError(() => new Error('boom')));
      await submit();

      await typeReason('Vacaciones');

      expect(element.querySelector('[role="alert"]')).toBeNull();
    });
  });

  describe('closing', () => {
    it('closes from the cancel button', () => {
      [...element.querySelectorAll<HTMLButtonElement>('button')]
        .find((b) => b.textContent?.trim() === 'Cancelar')!
        .click();
      expect(dialogRef.close).toHaveBeenCalledOnce();
    });
  });
});
