import { FormControl, FormGroup } from '@angular/forms';
import { availabilityEndAfterStart, exceptionEndAfterStart } from './availability.util';

function availabilityGroup(startTime: string, endTime: string) {
  return new FormGroup({
    startTime: new FormControl(startTime),
    endTime: new FormControl(endTime, availabilityEndAfterStart),
  });
}

function exceptionGroup(isFullDayBlock: boolean, startTime: string, endTime: string) {
  return new FormGroup({
    isFullDayBlock: new FormControl(isFullDayBlock),
    startTime: new FormControl(startTime),
    endTime: new FormControl(endTime, exceptionEndAfterStart),
  });
}

describe('availability.util', () => {
  describe('availabilityEndAfterStart', () => {
    it('is valid when the end is after the start', () => {
      const group = availabilityGroup('08:00', '12:00');
      expect(availabilityEndAfterStart(group.controls.endTime)).toBeNull();
    });

    it('flags an end equal to the start', () => {
      const group = availabilityGroup('08:00', '08:00');
      expect(availabilityEndAfterStart(group.controls.endTime)).toEqual({ endBeforeStart: true });
    });

    it('flags an end before the start', () => {
      const group = availabilityGroup('12:00', '08:00');
      expect(availabilityEndAfterStart(group.controls.endTime)).toEqual({ endBeforeStart: true });
    });

    it('is valid when either time is empty (required validators handle it)', () => {
      expect(availabilityEndAfterStart(availabilityGroup('', '08:00').controls.endTime)).toBeNull();
      expect(availabilityEndAfterStart(availabilityGroup('08:00', '').controls.endTime)).toBeNull();
    });

    it('is valid for a control without parent', () => {
      expect(availabilityEndAfterStart(new FormControl('08:00'))).toBeNull();
    });

    it('marks the control invalid inside a group after revalidation', () => {
      const group = availabilityGroup('08:00', '12:00');
      group.controls.startTime.setValue('13:00');
      group.controls.endTime.updateValueAndValidity();
      expect(group.controls.endTime.hasError('endBeforeStart')).toBe(true);
    });
  });

  describe('exceptionEndAfterStart', () => {
    it('is valid for a full day block regardless of times', () => {
      const group = exceptionGroup(true, '', '');
      expect(exceptionEndAfterStart(group.controls.endTime)).toBeNull();
    });

    it('is valid for a partial block with end after start', () => {
      const group = exceptionGroup(false, '09:00', '10:00');
      expect(exceptionEndAfterStart(group.controls.endTime)).toBeNull();
    });

    it('requires both times for a partial block', () => {
      expect(exceptionEndAfterStart(exceptionGroup(false, '', '10:00').controls.endTime)).toEqual({
        rangeRequired: true,
      });
      expect(exceptionEndAfterStart(exceptionGroup(false, '09:00', '').controls.endTime)).toEqual({
        rangeRequired: true,
      });
    });

    it('flags an end equal to or before the start for a partial block', () => {
      expect(
        exceptionEndAfterStart(exceptionGroup(false, '10:00', '10:00').controls.endTime),
      ).toEqual({ endBeforeStart: true });
      expect(
        exceptionEndAfterStart(exceptionGroup(false, '10:00', '09:00').controls.endTime),
      ).toEqual({ endBeforeStart: true });
    });

    it('is valid for a control without parent', () => {
      expect(exceptionEndAfterStart(new FormControl(''))).toBeNull();
    });
  });
});
