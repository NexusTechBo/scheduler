import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from '../http/api-error';
import { toHttpParams } from '../http/http-params.util';
import {
  addMinutesToTime,
  buildMonthMatrix,
  calendarKeyToIso,
  formatTime,
  startOfWeek,
  timeSlots,
  toIso,
  toLocalDateTime,
} from './calendar.util';
import { dayOfWeekOf, initials } from './labels.util';

describe('calendar.util', () => {
  it('formats backend LocalTime and LocalDateTime as HH:mm', () => {
    expect(formatTime('08:30:00')).toBe('08:30');
    expect(formatTime('2026-10-06T14:00:00')).toBe('14:00');
  });

  it('builds backend LocalDateTime without timezone shift', () => {
    expect(toLocalDateTime('2026-10-06', '08:00:00')).toBe('2026-10-06T08:00:00');
  });

  it('lists the day in step-minute slots', () => {
    const slots = timeSlots(30);
    expect(slots.length).toBe(48);
    expect(slots[0]).toBe('00:00');
    expect(slots[47]).toBe('23:30');
  });

  it('adds slot minutes across the hour', () => {
    expect(addMinutesToTime('08:45', 30)).toBe('09:15');
  });

  it('converts calendar keys (MM-dd-yyyy) to ISO', () => {
    expect(calendarKeyToIso('10-06-2026')).toBe('2026-10-06');
  });

  it('uses local dates for ISO strings', () => {
    expect(toIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('starts weeks on Monday and always renders 6 weeks', () => {
    expect(toIso(startOfWeek(new Date(2026, 9, 11)))).toBe('2026-10-05');
    const weeks = buildMonthMatrix(2026, 9);
    expect(weeks.length).toBe(6);
    expect(weeks[0][0].iso).toBe('2026-09-28');
  });
});

describe('labels.util', () => {
  it('maps JS days to backend DayOfWeek', () => {
    expect(dayOfWeekOf(new Date(2026, 9, 5))).toBe('MONDAY');
    expect(dayOfWeekOf(new Date(2026, 9, 11))).toBe('SUNDAY');
  });

  it('builds initials ignoring the doctor prefix', () => {
    expect(initials('Dr. Ana García')).toBe('AG');
    expect(initials(null)).toBe('?');
  });
});

describe('http helpers', () => {
  it('omits empty query params', () => {
    const params = toHttpParams({ a: 1, b: undefined, c: null, d: '', e: false });
    expect(params.keys()).toEqual(['a', 'e']);
  });

  it('translates known backend errors', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { status: 409, message: 'That slot is already taken', errors: null },
    });
    expect(apiErrorMessage(error)).toBe('Ese horario ya fue reservado, elige otro');
  });

  it('only mentions correo/CI for duplicates that are about them', () => {
    const duplicate = (message: string) =>
      apiErrorMessage(new HttpErrorResponse({ status: 409, error: { message } }));
    expect(duplicate('Account with email a@b.com is already exists')).toBe(
      'Ya existe un registro con ese correo o CI',
    );
    expect(duplicate('This specialty already exists')).toBe(
      'Ya existe un registro con esos datos.',
    );
  });

  it('prefers field validation messages and falls back per status', () => {
    const validation = new HttpErrorResponse({
      status: 400,
      error: {
        message: 'Validation failed',
        errors: [{ field: 'name', message: 'no debe ser nulo' }],
      },
    });
    expect(apiErrorMessage(validation)).toBe('no debe ser nulo');
    const emptyDetail = new HttpErrorResponse({
      status: 400,
      error: { errors: [{ field: 'name', message: null }] },
    });
    expect(apiErrorMessage(emptyDetail)).toBe('Revisa los datos ingresados');
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toBe(
      'No se pudo conectar con el servidor',
    );
  });
});
