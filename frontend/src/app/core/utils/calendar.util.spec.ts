import {
  addDays,
  addMinutesToTime,
  buildMonthMatrix,
  calendarKeyToIso,
  capitalize,
  datePart,
  formatDayLabel,
  formatLongDate,
  formatMonthLabel,
  formatShortDate,
  formatTime,
  formatTimeRange,
  formatWeekdayShort,
  isIsoDate,
  parseIso,
  startOfWeek,
  timeSlots,
  toIso,
  toLocalDateTime,
} from './calendar.util';

describe('calendar.util', () => {
  describe('capitalize', () => {
    it('uppercases the first character and keeps the rest', () => {
      expect(capitalize('jueves, 5 de noviembre')).toBe('Jueves, 5 de noviembre');
    });

    it('returns an empty string unchanged', () => {
      expect(capitalize('')).toBe('');
    });

    it('keeps an already capitalized text', () => {
      expect(capitalize('Lunes')).toBe('Lunes');
    });
  });

  describe('toIso and parseIso', () => {
    it('formats a local date as yyyy-MM-dd with zero padding', () => {
      expect(toIso(new Date(2026, 0, 5))).toBe('2026-01-05');
    });

    it('parses an ISO date as a local midnight date', () => {
      const date = parseIso('2026-11-05');
      expect(date.getFullYear()).toBe(2026);
      expect(date.getMonth()).toBe(10);
      expect(date.getDate()).toBe(5);
      expect(date.getHours()).toBe(0);
    });

    it('round-trips an ISO date', () => {
      expect(toIso(parseIso('2024-02-29'))).toBe('2024-02-29');
    });
  });

  describe('isIsoDate', () => {
    it('accepts a valid calendar date', () => {
      expect(isIsoDate('2026-11-05')).toBe(true);
    });

    it.each([null, undefined, '', '2026-1-5', '05-11-2026', '2026-02-30', '2026-13-01', 'abc'])(
      'rejects %s',
      (value) => {
        expect(isIsoDate(value)).toBe(false);
      },
    );
  });

  describe('addDays', () => {
    it('adds days across a month boundary without mutating the input', () => {
      const source = new Date(2026, 9, 30);
      const result = addDays(source, 3);
      expect(toIso(result)).toBe('2026-11-02');
      expect(toIso(source)).toBe('2026-10-30');
    });

    it('subtracts days with a negative amount', () => {
      expect(toIso(addDays(new Date(2026, 0, 1), -1))).toBe('2025-12-31');
    });
  });

  describe('startOfWeek', () => {
    it('returns the Monday of the week for a midweek day', () => {
      expect(toIso(startOfWeek(new Date(2026, 10, 5, 15, 30)))).toBe('2026-11-02');
    });

    it('returns the previous Monday for a Sunday', () => {
      expect(toIso(startOfWeek(new Date(2026, 10, 8)))).toBe('2026-11-02');
    });

    it('returns the same day at midnight for a Monday', () => {
      const result = startOfWeek(new Date(2026, 10, 2, 18));
      expect(toIso(result)).toBe('2026-11-02');
      expect(result.getHours()).toBe(0);
    });
  });

  describe('buildMonthMatrix', () => {
    it('builds 6 weeks of 7 days starting on Monday', () => {
      const weeks = buildMonthMatrix(2026, 10);
      expect(weeks).toHaveLength(6);
      expect(weeks.every((week) => week.length === 7)).toBe(true);
      expect(weeks[0][0].iso).toBe('2026-10-26');
      expect(weeks[0][0].date.getDay()).toBe(1);
    });

    it('flags days outside the current month', () => {
      const days = buildMonthMatrix(2026, 10).flat();
      expect(days.find((d) => d.iso === '2026-10-31')?.inCurrentMonth).toBe(false);
      expect(days.find((d) => d.iso === '2026-11-01')?.inCurrentMonth).toBe(true);
      expect(days.filter((d) => d.inCurrentMonth)).toHaveLength(30);
    });
  });

  describe('Spanish formatters', () => {
    it('formats a month label capitalized', () => {
      expect(formatMonthLabel(new Date(2026, 10, 1))).toBe('Noviembre de 2026');
    });

    it('formats a day label capitalized', () => {
      expect(formatDayLabel('2026-11-05')).toBe('Jue, 5 nov');
    });

    it('formats a long date capitalized', () => {
      expect(formatLongDate('2026-11-05')).toBe('Jueves, 5 de noviembre de 2026');
    });

    it('formats a short weekday without trailing dot', () => {
      expect(formatWeekdayShort(new Date(2026, 10, 5))).toBe('jue');
    });

    it('formats a short date', () => {
      expect(formatShortDate('2026-11-05')).toBe('5 nov');
    });
  });

  describe('formatTime', () => {
    it('keeps HH:mm from a time with seconds', () => {
      expect(formatTime('09:30:00')).toBe('09:30');
    });

    it('extracts the time from a local date time', () => {
      expect(formatTime('2026-11-05T14:15:00')).toBe('14:15');
    });

    it('formats a time range with an en dash', () => {
      expect(formatTimeRange('09:00:00', '2026-11-05T09:30:00')).toBe('09:00 – 09:30');
    });
  });

  describe('datePart and toLocalDateTime', () => {
    it('extracts the date of a local date time', () => {
      expect(datePart('2026-11-05T14:15:00')).toBe('2026-11-05');
    });

    it('builds a local date time with seconds from a date and a short time', () => {
      expect(toLocalDateTime('2026-11-05', '09:30')).toBe('2026-11-05T09:30:00');
    });

    it('normalizes a time that already has seconds', () => {
      expect(toLocalDateTime('2026-11-05', '09:30:45')).toBe('2026-11-05T09:30:00');
    });
  });

  describe('addMinutesToTime', () => {
    it('adds minutes within the same hour', () => {
      expect(addMinutesToTime('08:00', 30)).toBe('08:30');
    });

    it('carries over to the next hour', () => {
      expect(addMinutesToTime('08:45:00', 30)).toBe('09:15');
    });

    it('does not wrap past midnight', () => {
      expect(addMinutesToTime('23:45', 30)).toBe('24:15');
    });
  });

  describe('calendarKeyToIso', () => {
    it('converts an MM-dd-yyyy key to ISO', () => {
      expect(calendarKeyToIso('11-05-2026')).toBe('2026-11-05');
    });
  });

  describe('timeSlots', () => {
    it('lists every slot of the day for the given step', () => {
      const slots = timeSlots(30);
      expect(slots).toHaveLength(48);
      expect(slots[0]).toBe('00:00');
      expect(slots[1]).toBe('00:30');
      expect(slots.at(-1)).toBe('23:30');
    });

    it('treats a non-positive step as one minute', () => {
      expect(timeSlots(0)).toHaveLength(24 * 60);
    });
  });
});
