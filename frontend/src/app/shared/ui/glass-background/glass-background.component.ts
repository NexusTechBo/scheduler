import { Component } from '@angular/core';

@Component({
  selector: 'app-glass-background',
  template: `
    <div class="bg-blob -left-28 -top-56 h-140 w-140 bg-primary/20"></div>
    <div class="bg-blob -right-40 top-16 h-130 w-130 bg-secondary/22"></div>
    <div class="bg-blob -bottom-52 left-1/3 h-115 w-115 bg-tertiary/13"></div>
    <ng-content />
  `,
  host: {
    class: 'relative overflow-hidden bg-gradient-to-b from-[#eef2f7] via-[#f5f7fa] to-[#eef3f6]',
  },
})
export class GlassBackgroundComponent {}
