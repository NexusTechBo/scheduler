import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartData,
  ChartOptions,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';
import { STATUS_PLURAL_LABEL } from '../../../../core/utils/labels.util';
import { WeekDayStats } from '../../dashboard.model';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const GRAY = '#9ca3af';
const TEXT = '#6b7280';
const GRID = 'rgba(17, 24, 39, 0.06)';

@Component({
  selector: 'app-week-chart',
  template: `<canvas #canvas role="img" [attr.aria-label]="summary()"></canvas>`,
  host: { class: 'relative block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeekChartComponent {
  days = input.required<WeekDayStats[]>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart<'bar'>;

  protected readonly summary = computed(
    () =>
      'Citas de la semana. ' +
      this.days()
        .map(
          (day) =>
            `${day.weekday} ${day.day}: ${day.confirmed} confirmadas, ${day.pending} pendientes, ${day.cancelled} canceladas`,
        )
        .join('. '),
  );

  constructor() {
    afterNextRender(() => {
      this.chart = new Chart<'bar'>(this.canvas().nativeElement, {
        type: 'bar',
        data: this.toData(this.days()),
        options: this.options(),
      });
    });
    effect(() => {
      const data = this.toData(this.days());
      if (!this.chart) return;
      this.chart.data = data;
      this.chart.update();
    });
    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }

  private toData(days: WeekDayStats[]): ChartData<'bar'> {
    const colors = themeColors();
    const dataset = (label: string, color: string, value: (day: WeekDayStats) => number) => ({
      label,
      data: days.map(value),
      backgroundColor: color,
      borderRadius: 6,
      borderSkipped: false,
      maxBarThickness: 44,
      stack: 'week',
    });
    return {
      labels: days.map((day) => `${capitalize(day.weekday)} ${day.day}`),
      datasets: [
        dataset(STATUS_PLURAL_LABEL.CONFIRMED, colors.primary, (day) => day.confirmed),
        dataset(STATUS_PLURAL_LABEL.PENDING, colors.tertiary, (day) => day.pending),
        dataset(STATUS_PLURAL_LABEL.CANCELLED, GRAY, (day) => day.cancelled),
      ],
    };
  }

  private options(): ChartOptions<'bar'> {
    const colors = themeColors();
    const font = { family: colors.font, size: 12, weight: 600 } as const;
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 400 },
      interaction: { mode: 'index' as const, intersect: false },
      plugins: {
        legend: {
          position: 'bottom' as const,
          labels: { usePointStyle: true, pointStyle: 'circle', boxHeight: 8, color: TEXT, font },
        },
        tooltip: {
          titleFont: font,
          bodyFont: { ...font, weight: 500 },
          callbacks: {
            footer: (items: { parsed: { y: number | null } }[]) =>
              `Total: ${items.reduce((sum, item) => sum + (item.parsed.y ?? 0), 0)}`,
          },
        },
      },
      scales: {
        x: {
          stacked: true,
          grid: { display: false },
          border: { display: false },
          ticks: {
            font,
            color: (ctx: { index: number }) =>
              this.days()[ctx.index]?.isToday ? colors.primary : TEXT,
          },
        },
        y: {
          stacked: true,
          beginAtZero: true,
          border: { display: false },
          grid: { color: GRID },
          ticks: { precision: 0, color: TEXT, font: { ...font, weight: 500 } },
        },
      },
    };
  }
}

function themeColors() {
  const style = getComputedStyle(document.documentElement);
  const read = (name: string, fallback: string) => style.getPropertyValue(name).trim() || fallback;
  return {
    primary: read('--color-primary', '#0056b3'),
    tertiary: read('--color-tertiary', '#973c00'),
    font: read('--font-sans', 'Inter, sans-serif'),
  };
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
