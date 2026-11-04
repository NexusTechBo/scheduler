import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TimeSlot } from '../../../../core/models/api.models';
import { EmptyStateComponent } from '../../empty-state/empty-state.component';
import { LoadErrorComponent } from '../../load-error/load-error.component';
import { SkeletonComponent } from '../../skeleton/skeleton.component';

@Component({
  selector: 'app-slot-grid',
  imports: [EmptyStateComponent, LoadErrorComponent, SkeletonComponent],
  templateUrl: './slot-grid.component.html',
  host: { class: 'block', 'aria-live': 'polite', '[attr.aria-busy]': 'loading()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlotGridComponent {
  readonly slots = input.required<TimeSlot[]>();
  readonly selected = input<TimeSlot | null>(null);
  readonly loading = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly emptyTitle = input('Sin horarios libres');
  readonly emptyMessage = input<string>();
  readonly select = output<TimeSlot>();
  readonly retry = output<void>();
}
