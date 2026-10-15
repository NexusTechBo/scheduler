import { Component, input, output } from '@angular/core';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

/** Zero-based page index, like Spring's Pageable. */
@Component({
  selector: 'app-pagination',
  imports: [UiIconComponent],
  template: `
    @if (totalPages() > 1) {
      <span class="text-sm font-medium text-gray-500">
        Página <b class="text-gray-800">{{ page() + 1 }}</b> de {{ totalPages() }} · {{ totalElements() }} registros
      </span>
      <div class="flex items-center gap-1.5">
        <button type="button" class="btn-icon" [disabled]="page() === 0" (click)="pageChange.emit(page() - 1)" aria-label="Página anterior">
          <app-ui-icon name="chevronLeft" class="h-4 w-4" />
        </button>
        <button
          type="button"
          class="btn-icon"
          [disabled]="page() >= totalPages() - 1"
          (click)="pageChange.emit(page() + 1)"
          aria-label="Página siguiente"
        >
          <app-ui-icon name="chevronRight" class="h-4 w-4" />
        </button>
      </div>
    }
  `,
  host: { class: 'flex items-center justify-between gap-4 empty:hidden' }
})
export class PaginationComponent {
  page = input.required<number>();
  totalPages = input.required<number>();
  totalElements = input(0);

  pageChange = output<number>();
}
