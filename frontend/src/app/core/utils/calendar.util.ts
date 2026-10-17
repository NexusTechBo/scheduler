export interface CalendarDay {
  date: Date;
  iso: string;
  inCurrentMonth: boolean;
}

const LOCALE = 'es-ES';

function pad(value: number): string {
  return value.toString().padStart(2, '0');
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function isIsoDate(value: string | null | undefined): value is string {
  return !!value && /^\d{4}-\d{2}-\d{2}$/.test(value) && toIso(parseIso(value)) === value;
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function startOfWeek(date: Date): Date {
  return addDays(
    new Date(date.getFullYear(), date.getMonth(), date.getDate()),
    -((date.getDay() + 6) % 7),
  );
}

export function buildMonthMatrix(year: number, month: number): CalendarDay[][] {
  const firstOfMonth = new Date(year, month, 1);
  const leadingDays = (firstOfMonth.getDay() + 6) % 7;
  const cursor = new Date(year, month, 1 - leadingDays);

  const weeks: CalendarDay[][] = [];
  for (let week = 0; week < 6; week++) {
    const days: CalendarDay[] = [];
    for (let day = 0; day < 7; day++) {
      days.push({
        date: new Date(cursor),
        iso: toIso(cursor),
        inCurrentMonth: cursor.getMonth() === month,
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    weeks.push(days);
  }

  return weeks;
}

export function formatMonthLabel(date: Date): string {
  return capitalize(date.toLocaleDateString(LOCALE, { month: 'long', year: 'numeric' }));
}

export function formatDayLabel(iso: string): string {
  return capitalize(
    parseIso(iso).toLocaleDateString(LOCALE, { weekday: 'short', month: 'short', day: 'numeric' }),
  );
}

export function formatLongDate(iso: string): string {
  return capitalize(
    parseIso(iso).toLocaleDateString(LOCALE, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }),
  );
}

export function formatShortDate(iso: string): string {
  return parseIso(iso).toLocaleDateString(LOCALE, { month: 'short', day: 'numeric' });
}

export function formatTime(value: string): string {
  const time = value.includes('T') ? value.split('T')[1] : value;
  return time.slice(0, 5);
}

export function datePart(dateTime: string): string {
  return dateTime.slice(0, 10);
}

export function toLocalDateTime(dateIso: string, time: string): string {
  return `${dateIso}T${formatTime(time)}:00`;
}

export function addMinutesToTime(time: string, minutes: number): string {
  const [hours, mins] = formatTime(time).split(':').map(Number);
  const total = hours * 60 + mins + minutes;
  return `${pad(Math.floor(total / 60))}:${pad(total % 60)}`;
}

export function calendarKeyToIso(key: string): string {
  const [month, day, year] = key.split('-');
  return `${year}-${month}-${day}`;
}

export function timeSlots(step: number): string[] {
  const slots: string[] = [];
  for (let minutes = 0; minutes < 24 * 60; minutes += Math.max(step, 1)) {
    slots.push(`${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`);
  }
  return slots;
}
