import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { UiIconComponent } from '../ui-icon/ui-icon.component';

@Component({
  selector: 'app-pagination',
  imports: [UiIconComponent],
  templateUrl: './pagination.component.html',
  host: { class: 'flex items-center justify-between gap-4 empty:hidden' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
  page = input.required<number>();
  totalPages = input.required<number>();
  totalElements = input(0);

  pageChange = output<number>();
}
