import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LandingShot } from '../../constants/landing.content';

@Component({
  selector: 'app-browser-frame',
  template: `
    <div
      class="overflow-hidden rounded-2xl border border-white/70 bg-white/70 shadow-2xl shadow-primary/15 ring-1 ring-gray-900/5 backdrop-blur"
    >
      <div class="flex items-center gap-1.5 border-b border-gray-900/5 bg-white/80 px-4 py-2.5">
        <span class="h-2.5 w-2.5 rounded-full bg-red-400/80"></span>
        <span class="h-2.5 w-2.5 rounded-full bg-amber-400/80"></span>
        <span class="h-2.5 w-2.5 rounded-full bg-emerald-400/80"></span>
        <span
          class="ml-3 hidden flex-1 truncate rounded-md bg-gray-900/[0.04] px-3 py-1 text-[11px] font-medium text-gray-400 sm:block"
          >app.clinicflow.com</span
        >
      </div>
      <img
        class="block h-auto w-full"
        [src]="shot().src"
        [alt]="shot().alt"
        width="1600"
        height="1000"
        [attr.loading]="eager() ? 'eager' : 'lazy'"
        [attr.fetchpriority]="eager() ? 'high' : null"
        decoding="async"
      />
    </div>
  `,
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrowserFrameComponent {
  shot = input.required<LandingShot>();
  eager = input(false);
}
