import { HttpErrorResponse } from '@angular/common/http';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, EMPTY, Subject, of, throwError } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { BoardSocket } from '../../core/api/board.socket';
import { AppointmentBoard, AppointmentSummaryItem } from '../../core/models/api.models';
import { addDays, toIso } from '../../core/utils/calendar.util';
import { AppointmentActionsService } from '../../shared/services/appointment-actions.service';
import { NotificationService } from '../../shared/services/notification.service';
import { BoardService } from './board.service';

const TODAY = toIso(new Date());
const YESTERDAY = toIso(addDays(new Date(), -1));
const TOMORROW = toIso(addDays(new Date(), 1));

function item(overrides: Partial<AppointmentSummaryItem> = {}): AppointmentSummaryItem {
  return {
    id: 1,
    clientName: 'Ana Pérez',
    doctorName: 'Dr. Gómez',
    appointmentDate: TOMORROW,
    startTime: '09:30:00',
    endTime: '10:00:00',
    status: 'PENDING',
    doctorId: 10,
    patientId: 20,
    ...overrides,
  };
}

function board(overrides: Partial<AppointmentBoard> = {}): AppointmentBoard {
  return { PENDING: [], CONFIRMED: [], CANCELLED: [], ...overrides };
}

class FakeBoardSocket {
  readonly events = new Subject<unknown>();
  readonly connected = new BehaviorSubject<unknown>(true);
  readonly events$ = this.events.asObservable();
  readonly connected$ = this.connected.asObservable();
  connectedNow = true;
  isConnected(): boolean {
    return this.connectedNow;
  }
}

describe('BoardService', () => {
  let service: BoardService;
  let api: { board: ReturnType<typeof vi.fn> };
  let actions: {
    confirm: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    openDetail: ReturnType<typeof vi.fn>;
  };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  let socket: FakeBoardSocket;

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function setup(): void {
    api = { board: vi.fn(() => of(board())) };
    actions = { confirm: vi.fn(() => EMPTY), cancel: vi.fn(() => EMPTY), openDetail: vi.fn() };
    notifications = { success: vi.fn(), error: vi.fn() };
    socket = new FakeBoardSocket();
    TestBed.configureTestingModule({
      providers: [
        BoardService,
        { provide: AppointmentsApi, useValue: api },
        { provide: AppointmentActionsService, useValue: actions },
        { provide: NotificationService, useValue: notifications },
        { provide: BoardSocket, useValue: socket },
      ],
    });
    service = TestBed.inject(BoardService);
  }

  afterEach(() => vi.useRealTimers());

  describe('loading', () => {
    it('loads the next 7 days by default', async () => {
      setup();
      await settle();
      expect(service.range()).toEqual({ from: TODAY, to: toIso(addDays(new Date(), 6)) });
      expect(api.board).toHaveBeenCalledWith({ from: TODAY, to: toIso(addDays(new Date(), 6)) });
      expect(service.hasData()).toBe(true);
      expect(service.showDates()).toBe(true);
    });

    it('reloads with the new range and hides dates for a single day', async () => {
      setup();
      await settle();
      service.setRange({ from: TOMORROW, to: TOMORROW });
      await settle();
      expect(api.board).toHaveBeenLastCalledWith({ from: TOMORROW, to: TOMORROW });
      expect(service.showDates()).toBe(false);
    });

    it('exposes the API error message', async () => {
      setup();
      api.board.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Caído' } })),
      );
      service.reload();
      await settle();
      expect(service.errorMessage()).toBe('Caído');
    });

    it('uses the board fallback for unknown errors', async () => {
      setup();
      api.board.mockReturnValue(throwError(() => new Error('boom')));
      service.reload();
      await settle();
      expect(service.errorMessage()).toBe('No se pudo cargar el tablero');
    });
  });

  describe('columns', () => {
    it('builds one empty column per status before data arrives', () => {
      setup();
      const columns = service.columns();
      expect(columns.map((c) => c.id)).toEqual(['PENDING', 'CONFIRMED', 'CANCELLED']);
      expect(columns.map((c) => c.title)).toEqual(['Pendientes', 'Confirmadas', 'Canceladas']);
      expect(columns.every((c) => c.appointments.length === 0)).toBe(true);
    });

    it('fills the columns and flags appointments that already started', async () => {
      setup();
      const past = item({ id: 1, appointmentDate: YESTERDAY });
      const future = item({ id: 2, appointmentDate: TOMORROW });
      const confirmedPast = item({ id: 3, status: 'CONFIRMED', appointmentDate: YESTERDAY });
      api.board.mockReturnValue(of(board({ PENDING: [past, future], CONFIRMED: [confirmedPast] })));
      service.reload();
      await settle();

      const [pending, confirmed, cancelled] = service.columns();
      expect(pending.appointments).toEqual([past, future]);
      expect(pending.pastIds).toEqual({ 1: true });
      expect(confirmed.pastIds).toEqual({ 3: true });
      expect(cancelled.appointments).toEqual([]);
      expect(pending.accentClass).toBe('bg-tertiary');
    });

    it('copies the arrays so the page can reorder them locally', async () => {
      setup();
      const pending = [item()];
      api.board.mockReturnValue(of(board({ PENDING: pending })));
      service.reload();
      await settle();
      expect(service.columns()[0].appointments).not.toBe(pending);
    });
  });

  describe('realtime', () => {
    beforeEach(() => {
      vi.useFakeTimers({
        toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'],
      });
    });

    it('reloads once after a burst of socket events', async () => {
      setup();
      await settle();
      api.board.mockClear();

      socket.events.next({});
      vi.advanceTimersByTime(100);
      socket.events.next({});
      vi.advanceTimersByTime(299);
      await settle();
      expect(api.board).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1);
      await settle();
      expect(api.board).toHaveBeenCalledOnce();
    });

    it('ignores the initial connection state and reloads on reconnection', async () => {
      setup();
      await settle();
      api.board.mockClear();

      vi.advanceTimersByTime(300);
      await settle();
      expect(api.board).not.toHaveBeenCalled();

      socket.connected.next(true);
      vi.advanceTimersByTime(300);
      await settle();
      expect(api.board).toHaveBeenCalledOnce();
    });

    it('stops listening when destroyed', async () => {
      setup();
      await settle();
      TestBed.resetTestingModule();
      expect(socket.events.observed).toBe(false);
    });
  });

  describe('move', () => {
    const future = (overrides: Partial<AppointmentSummaryItem> = {}) =>
      item({ appointmentDate: TOMORROW, ...overrides });
    const past = (overrides: Partial<AppointmentSummaryItem> = {}) =>
      item({ appointmentDate: YESTERDAY, ...overrides });

    it('confirms a future pending appointment through the shared actions', () => {
      setup();
      service.move(future({ id: 5 }), 'CONFIRMED');
      expect(actions.confirm).toHaveBeenCalledWith(5, 'Ana Pérez', `${TOMORROW}T09:30:00`);
    });

    it('rejects confirming a past appointment', () => {
      setup();
      service.move(past(), 'CONFIRMED');
      expect(actions.confirm).not.toHaveBeenCalled();
      expect(notifications.error).toHaveBeenCalledWith(
        'No se puede modificar una cita que ya pasó',
      );
    });

    it.each(['PENDING', 'CONFIRMED'] as const)('cancels a future %s appointment', (status) => {
      setup();
      service.move(future({ id: 6, status }), 'CANCELLED');
      expect(actions.cancel).toHaveBeenCalledWith(6, 'Ana Pérez', `${TOMORROW}T09:30:00`);
    });

    it('rejects cancelling a past appointment', () => {
      setup();
      service.move(past({ status: 'CONFIRMED' }), 'CANCELLED');
      expect(actions.cancel).not.toHaveBeenCalled();
      expect(notifications.error).toHaveBeenCalledWith(
        'No se puede modificar una cita que ya pasó',
      );
    });

    it('rejects moving back to pending', () => {
      setup();
      service.move(future({ status: 'CONFIRMED' }), 'PENDING');
      expect(notifications.error).toHaveBeenCalledWith('Una cita no puede volver a pendiente');
    });

    it('rejects reactivating a cancelled appointment', () => {
      setup();
      service.move(future({ status: 'CANCELLED' }), 'CONFIRMED');
      expect(actions.confirm).not.toHaveBeenCalled();
      expect(notifications.error).toHaveBeenCalledWith('Una cita cancelada no puede reactivarse');
    });

    it('does not reload after an action while the socket is connected', async () => {
      setup();
      await settle();
      api.board.mockClear();
      actions.confirm.mockReturnValue(of({}));

      service.move(future(), 'CONFIRMED');
      await settle();

      expect(api.board).not.toHaveBeenCalled();
    });

    it('reloads after an action when the socket is disconnected', async () => {
      setup();
      await settle();
      api.board.mockClear();
      socket.connectedNow = false;
      actions.cancel.mockReturnValue(of({}));

      service.move(future(), 'CANCELLED');
      await settle();

      expect(api.board).toHaveBeenCalledOnce();
    });
  });

  describe('open', () => {
    it('reloads after the detail dialog reports a change when offline', async () => {
      setup();
      await settle();
      api.board.mockClear();
      socket.connectedNow = false;
      actions.openDetail.mockReturnValue(of(undefined));

      service.open(item({ id: 9 }));
      await settle();

      expect(actions.openDetail).toHaveBeenCalledWith(9);
      expect(api.board).toHaveBeenCalledOnce();
    });

    it('does not reload when the detail dialog closes without changes', async () => {
      setup();
      await settle();
      api.board.mockClear();
      socket.connectedNow = false;
      actions.openDetail.mockReturnValue(EMPTY);

      service.open(item());
      await settle();

      expect(api.board).not.toHaveBeenCalled();
    });
  });
});
