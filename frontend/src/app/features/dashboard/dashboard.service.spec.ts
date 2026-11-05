import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EMPTY, of, throwError } from 'rxjs';
import { AppointmentsApi } from '../../core/api/appointments.api';
import { AppointmentBoard, AppointmentSummaryItem } from '../../core/models/api.models';
import { AppointmentActionsService } from '../../shared/services/appointment-actions.service';
import { DashboardService } from './dashboard.service';

const NOW = new Date(2026, 10, 5, 10, 0, 0);
const TODAY = '2026-11-05';

function item(overrides: Partial<AppointmentSummaryItem> = {}): AppointmentSummaryItem {
  return {
    id: 1,
    clientName: 'Ana Pérez',
    doctorName: 'Dr. Gómez',
    appointmentDate: TODAY,
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

describe('DashboardService', () => {
  let service: DashboardService;
  let api: { board: ReturnType<typeof vi.fn> };
  let actions: {
    confirm: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    openDetail: ReturnType<typeof vi.fn>;
  };

  async function settle(): Promise<void> {
    TestBed.tick();
    await TestBed.inject(ApplicationRef).whenStable();
  }

  async function setup(data: AppointmentBoard = board()): Promise<void> {
    api = { board: vi.fn(() => of(data)) };
    actions = { confirm: vi.fn(() => EMPTY), cancel: vi.fn(() => EMPTY), openDetail: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        { provide: AppointmentsApi, useValue: api },
        { provide: AppointmentActionsService, useValue: actions },
      ],
    });
    service = TestBed.inject(DashboardService);
    await settle();
  }

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(NOW);
  });

  afterEach(() => vi.useRealTimers());

  describe('loading', () => {
    it('loads the board for the current Monday to Sunday week', async () => {
      await setup();
      expect(api.board).toHaveBeenCalledWith({ from: '2026-11-02', to: '2026-11-08' });
    });

    it('exposes null KPI values and an error message when loading fails', async () => {
      TestBed.configureTestingModule({
        providers: [
          DashboardService,
          { provide: AppointmentsApi, useValue: { board: () => throwError(() => new Error('x')) } },
          { provide: AppointmentActionsService, useValue: {} },
        ],
      });
      service = TestBed.inject(DashboardService);
      await settle();

      expect(service.weekErrorMessage()).toBe('No se pudo cargar la agenda');
      expect(service.kpis().map((k) => k.value)).toEqual([null, null, null, null]);
      expect(service.todayAppointments()).toEqual([]);
    });
  });

  describe('derived data', () => {
    const data = board({
      PENDING: [
        item({ id: 1, startTime: '11:00:00' }),
        item({ id: 2, appointmentDate: '2026-11-03' }),
        item({ id: 3, appointmentDate: '2026-11-07', startTime: '08:00:00' }),
        item({ id: 4, appointmentDate: '2026-11-06', startTime: '15:00:00' }),
      ],
      CONFIRMED: [item({ id: 5, status: 'CONFIRMED', startTime: '08:30:00' })],
      CANCELLED: [item({ id: 6, status: 'CANCELLED', startTime: '07:00:00' })],
    });

    it("lists today's active appointments sorted by start time", async () => {
      await setup(data);
      expect(service.todayAppointments().map((a) => a.id)).toEqual([5, 1]);
    });

    it('counts and lists upcoming pending appointments sorted by date and time', async () => {
      await setup(data);
      expect(service.pendingCount()).toBe(3);
      expect(service.pendingToConfirm().map((p) => [p.appointment.id, p.dayLabel])).toEqual([
        [1, 'Hoy'],
        [4, '6 nov'],
        [3, '7 nov'],
      ]);
    });

    it('limits the pending list to 6 items', async () => {
      const many = Array.from({ length: 8 }, (_, i) =>
        item({ id: i + 1, startTime: `1${i}:00:00` }),
      );
      await setup(board({ PENDING: many }));
      expect(service.pendingCount()).toBe(8);
      expect(service.pendingToConfirm()).toHaveLength(6);
    });

    it('builds 7 week days with per status counts', async () => {
      await setup(data);
      const days = service.weekDays();
      expect(days.map((d) => d.iso)).toEqual([
        '2026-11-02',
        '2026-11-03',
        '2026-11-04',
        '2026-11-05',
        '2026-11-06',
        '2026-11-07',
        '2026-11-08',
      ]);
      const today = days[3];
      expect(today).toMatchObject({
        weekday: 'jue',
        day: 5,
        isToday: true,
        confirmed: 1,
        pending: 1,
        cancelled: 1,
        total: 3,
      });
      expect(days.filter((d) => d.isToday)).toHaveLength(1);
      expect(service.weekTotal()).toBe(6);
    });

    it('computes the KPIs from the week board', async () => {
      await setup(data);
      expect(service.kpis().map((k) => [k.label, k.value])).toEqual([
        ['Citas hoy', 2],
        ['Pendientes (semana)', 4],
        ['Confirmadas (semana)', 1],
        ['Canceladas (semana)', 1],
      ]);
    });
  });

  describe('commands', () => {
    it('confirms with the appointment start date time and reloads on success', async () => {
      await setup();
      api.board.mockClear();
      actions.confirm.mockReturnValue(of({}));

      service.confirm(item({ id: 7, startTime: '14:00:00' }));
      await settle();

      expect(actions.confirm).toHaveBeenCalledWith(7, 'Ana Pérez', `${TODAY}T14:00:00`);
      expect(api.board).toHaveBeenCalledOnce();
    });

    it('cancels with the appointment start date time and reloads on success', async () => {
      await setup();
      api.board.mockClear();
      actions.cancel.mockReturnValue(of({}));

      service.cancel(item({ id: 8 }));
      await settle();

      expect(actions.cancel).toHaveBeenCalledWith(8, 'Ana Pérez', `${TODAY}T09:30:00`);
      expect(api.board).toHaveBeenCalledOnce();
    });

    it('does not reload when the action is dismissed', async () => {
      await setup();
      api.board.mockClear();

      service.confirm(item());
      service.cancel(item());
      await settle();

      expect(api.board).not.toHaveBeenCalled();
    });

    it('reloads after the detail dialog reports a change', async () => {
      await setup();
      api.board.mockClear();
      actions.openDetail.mockReturnValue(of(undefined));

      service.open(item({ id: 3 }));
      await settle();

      expect(actions.openDetail).toHaveBeenCalledWith(3);
      expect(api.board).toHaveBeenCalledOnce();
    });
  });
});
