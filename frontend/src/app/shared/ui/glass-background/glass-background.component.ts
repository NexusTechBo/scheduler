import { Component } from '@angular/core';

/** App-wide gradient + blurred blobs. Layout classes (display, size, padding) go on the host from the caller. */
@Component({
  selector: 'app-glass-background',
  template: `
    <div class="bg-blob -left-28 -top-56 h-[560px] w-[560px] bg-primary/20"></div>
    <div class="bg-blob -right-40 top-16 h-[520px] w-[520px] bg-secondary/[0.22]"></div>
    <div class="bg-blob -bottom-52 left-1/3 h-[460px] w-[460px] bg-tertiary/[0.13]"></div>
    <ng-content />
  `,
  host: {
    class: 'relative overflow-hidden bg-gradient-to-b from-[#eef2f7] via-[#f5f7fa] to-[#eef3f6]',
  },
})
export class GlassBackgroundComponent {}
