import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  host: {
    class: 'block rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700',
    role: 'alert',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertComponent {}
