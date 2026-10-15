import { Component, input, output } from '@angular/core';

export interface SegmentedTabItem {
  id: string;
  label: string;
}

@Component({
  selector: 'app-segmented-tabs',
  imports: [],
  templateUrl: './segmented-tabs.component.html',
  host: {
    class: 'glass-panel flex max-w-full items-center gap-1 overflow-x-auto rounded-2xl p-1.5',
  },
})
export class SegmentedTabsComponent {
  items = input.required<SegmentedTabItem[]>();
  selectedId = input.required<string>();

  select = output<string>();
}
