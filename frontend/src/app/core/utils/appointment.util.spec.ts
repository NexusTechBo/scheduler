import { AppointmentSummaryItem } from '../models/api.models';
import {
  appointmentStartDateTime,
  canCancelAppointment,
  canConfirmAppointment,
  hasAppointmentStarted,
} from './appointment.util';

function summaryItem(overrides: Partial<AppointmentSummaryItem> = {}): AppointmentSummaryItem {
  return {
    id: 1,
    clientName: 'Ana Pérez',
    doctorName: 'Dr. Gómez',
    appointmentDate: '2026-11-05',
    startTime: '09:30:00',
    endTime: '10:00:00',
    status: 'PENDING',
    doctorId: 10,
    patientId: 20,
    ...overrides,
  };
}

describe('appointment.util', () => {
  const NOW = new Date(2026, 10, 5, 10, 0, 0);

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('appointmentStartDateTime', () => {
    it('combines the appointment date and start time as a local date time', () => {
      expect(appointmentStartDateTime(summaryItem())).toBe('2026-11-05T09:30:00');
    });

    it('accepts a start time without seconds', () => {
      expect(appointmentStartDateTime(summaryItem({ startTime: '14:05' }))).toBe(
        '2026-11-05T14:05:00',
      );
    });
  });

  describe('hasAppointmentStarted', () => {
    it('is true for a start time in the past', () => {
      expect(hasAppointmentStarted('2026-11-05T09:59:00')).toBe(true);
    });

    it('is true exactly at the start time', () => {
      expect(hasAppointmentStarted('2026-11-05T10:00:00')).toBe(true);
    });

    it('is false for a start time in the future', () => {
      expect(hasAppointmentStarted('2026-11-05T10:01:00')).toBe(false);
    });
  });

  describe('canConfirmAppointment', () => {
    it('allows confirming a future pending appointment', () => {
      expect(canConfirmAppointment('PENDING', '2026-11-06T09:00:00')).toBe(true);
    });

    it('rejects a past pending appointment', () => {
      expect(canConfirmAppointment('PENDING', '2026-11-04T09:00:00')).toBe(false);
    });

    it.each(['CONFIRMED', 'CANCELLED'] as const)('rejects a future %s appointment', (status) => {
      expect(canConfirmAppointment(status, '2026-11-06T09:00:00')).toBe(false);
    });
  });

  describe('canCancelAppointment', () => {
    it.each(['PENDING', 'CONFIRMED'] as const)('allows cancelling a future %s appointment', (s) => {
      expect(canCancelAppointment(s, '2026-11-06T09:00:00')).toBe(true);
    });

    it('rejects an already cancelled appointment', () => {
      expect(canCancelAppointment('CANCELLED', '2026-11-06T09:00:00')).toBe(false);
    });

    it('rejects a past appointment', () => {
      expect(canCancelAppointment('CONFIRMED', '2026-11-04T09:00:00')).toBe(false);
    });
  });
});
