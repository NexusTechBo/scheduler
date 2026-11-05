import { provideZoneChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Chart } from 'chart.js';
import { WeekDayStats } from '../../dashboard.model';
import { WeekChartComponent } from './week-chart.component';

function day(overrides: Partial<WeekDayStats> = {}): WeekDayStats {
  return {
    iso: '2026-11-02',
    weekday: 'lun',
    day: 2,
    isToday: false,
    confirmed: 1,
    pending: 2,
    cancelled: 0,
    total: 3,
    ...overrides,
  };
}

function latestChart(): Chart<'bar'> {
  const charts = Object.values(Chart.instances) as Chart<'bar'>[];
  return charts[charts.length - 1];
}

describe('WeekChartComponent', () => {
  let fixture: ComponentFixture<WeekChartComponent>;
  let element: HTMLElement;
  let update: ReturnType<typeof vi.spyOn>;
  let destroy: ReturnType<typeof vi.spyOn>;

  const days = [
    day(),
    day({ iso: '2026-11-03', weekday: 'mar', day: 3, confirmed: 0, pending: 0, cancelled: 4 }),
  ];

  beforeEach(async () => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    update = vi.spyOn(Chart.prototype, 'update').mockImplementation(() => undefined);
    destroy = vi.spyOn(Chart.prototype, 'destroy').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      imports: [WeekChartComponent],
      providers: [provideZoneChangeDetection()],
    });
    fixture = TestBed.createComponent(WeekChartComponent);
    fixture.componentRef.setInput('days', days);
    fixture.autoDetectChanges();
    element = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => vi.restoreAllMocks());

  it('renders the canvas as an image with an accessible summary', () => {
    const canvas = element.querySelector('canvas')!;
    expect(canvas.getAttribute('role')).toBe('img');
    expect(canvas.getAttribute('aria-label')).toBe(
      'Citas de la semana. lun 2: 1 confirmadas, 2 pendientes, 0 canceladas. ' +
        'mar 3: 0 confirmadas, 0 pendientes, 4 canceladas',
    );
  });

  it('creates a stacked bar chart with capitalized day labels', () => {
    const chart = latestChart();
    expect((chart.config as { type?: string }).type).toBe('bar');
    expect(chart.data.labels).toEqual(['Lun 2', 'Mar 3']);
  });

  it('builds one dataset per status in confirmed, pending, cancelled order', () => {
    const datasets = latestChart().data.datasets;
    expect(datasets.map((d) => d.label)).toEqual(['Confirmadas', 'Pendientes', 'Canceladas']);
    expect(datasets.map((d) => d.data)).toEqual([
      [1, 0],
      [2, 0],
      [0, 4],
    ]);
    expect(datasets.every((d) => (d as { stack?: string }).stack === 'week')).toBe(true);
  });

  it('updates the chart and the summary when the days change', async () => {
    update.mockClear();
    fixture.componentRef.setInput('days', [day({ weekday: 'mié', day: 4, confirmed: 5 })]);
    await fixture.whenStable();

    expect(update).toHaveBeenCalled();
    expect(latestChart().data.labels).toEqual(['Mié 4']);
    expect(element.querySelector('canvas')!.getAttribute('aria-label')).toContain(
      'mié 4: 5 confirmadas',
    );
  });

  it('destroys the chart with the component', () => {
    fixture.destroy();
    expect(destroy).toHaveBeenCalled();
  });
});
