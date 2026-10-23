import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-glass-background',
  templateUrl: './glass-background.component.html',
  host: {
    class: 'relative overflow-hidden bg-gradient-to-b from-[#eef2f7] via-[#f5f7fa] to-[#eef3f6]',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlassBackgroundComponent {}
