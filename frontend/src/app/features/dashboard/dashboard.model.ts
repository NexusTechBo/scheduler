import { UiIconName } from '../../shared/ui/ui-icon/ui-icon.component';

export interface Kpi {
  label: string;
  value: number | null;
  icon: UiIconName;
  tone: string;
}

export interface WeekDayStats {
  iso: string;
  weekday: string;
  day: number;
  isToday: boolean;
  confirmed: number;
  pending: number;
  cancelled: number;
  total: number;
}
