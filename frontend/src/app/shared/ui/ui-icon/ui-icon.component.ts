import { Component, computed, input } from '@angular/core';

/**
 * Inline stroke icons (24x24, currentColor). Prefer this over pasting <svg> in templates.
 * For full-color/brand SVG files use `<app-icon>` + core/icons/icon-registry.ts instead.
 */
const PATHS = {
  home: 'M3 10.5 12 3l9 7.5M5 9v11a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9',
  board: 'M9 3v18M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z',
  calendar:
    'M8 2v4M16 2v4M3.5 9h17M4 4.5h16A1.5 1.5 0 0 1 21.5 6v14A1.5 1.5 0 0 1 20 21.5H4A1.5 1.5 0 0 1 2.5 20V6A1.5 1.5 0 0 1 4 4.5Z',
  list: 'M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01',
  patients:
    'M17 20v-1.5a3.5 3.5 0 0 0-3.5-3.5h-5A3.5 3.5 0 0 0 5 18.5V20M11 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19 20v-1.5a3 3 0 0 0-2.2-2.9M15.5 4.6a3 3 0 0 1 0 5.8',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM12 7v5l3.2 2',
  staff: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 8 0 0 1 16 0M12 15v4M10 17h4',
  tag: 'M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9-9-9ZM7.5 7.5h.01',
  chat: 'M21 12a8 8 0 0 1-11.6 7.1L4 20.5l1.4-4.6A8 8 0 1 1 21 12Z',
  sparkles:
    'M12 3l1.8 4.7L18.5 9.5l-4.7 1.8L12 16l-1.8-4.7L5.5 9.5l4.7-1.8L12 3ZM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15Z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 8 0 0 1 16 0',
  logout: 'M9 21H5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h4M16 17l5-5-5-5M21 12H9',
  plus: 'M12 5v14M5 12h14',
  check: 'm5 12.5 4.5 4.5L19 7.5',
  x: 'M6 18 18 6M6 6l12 12',
  chevronLeft: 'm15 18-6-6 6-6',
  chevronRight: 'm9 18 6-6-6-6',
  chevronDown: 'm6 9 6 6 6-6',
  trash:
    'M4 7h16M10 11v6M14 11v6M5 7l1 13a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1l1-13M9 7V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v3',
  edit: 'M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16v4ZM13.5 6.5l4 4',
  search: 'm21 21-4.3-4.3M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
  send: 'M5 12 3 4l18 8-18 8 2-8Zm0 0h7',
  link: 'M10 14a4.5 4.5 0 0 0 6.4 0l3.2-3.2a4.5 4.5 0 0 0-6.4-6.4L12 5.6M14 10a4.5 4.5 0 0 0-6.4 0l-3.2 3.2a4.5 4.5 0 0 0 6.4 6.4l1.2-1.2',
  block: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM5.6 5.6l12.8 12.8',
  stethoscope:
    'M6 3v6a4 4 0 0 0 8 0V3M10 13v2a5 5 0 0 0 10 0v-2M20 13a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z',
  mail: 'M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Zm-1 1 9 7 9-7',
  phone:
    'M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z',
  alert:
    'M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z',
  refresh: 'M20 11a8 8 0 0 0-14.7-4.3L3 9M3 4v5h5M4 13a8 8 0 0 0 14.7 4.3L21 15M21 20v-5h-5',
  menu: 'M4 6h16M4 12h16M4 18h16',
} as const;

export type UiIconName = keyof typeof PATHS;

@Component({
  selector: 'app-ui-icon',
  template: `<svg
    class="h-full w-full"
    fill="none"
    viewBox="0 0 24 24"
    stroke="currentColor"
    [attr.stroke-width]="strokeWidth()"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path [attr.d]="path()" />
  </svg>`,
  host: { class: 'inline-block shrink-0' },
})
export class UiIconComponent {
  name = input.required<UiIconName>();
  strokeWidth = input(1.9);

  protected readonly path = computed(() => PATHS[this.name()]);
}
