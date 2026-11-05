import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { AppointmentSummaryItem } from '../../../../core/models/api.models';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton.component';
import { BoardColumn } from '../../board.model';
import { BoardCardComponent } from '../board-card/board-card.component';

@Component({
  selector: 'app-board-column',
  imports: [CdkDropList, CdkDrag, BoardCardComponent, SkeletonComponent, TranslatePipe],
  templateUrl: './board-column.component.html',
  host: {
    class:
      'glass-panel flex w-[85vw] max-w-80 shrink-0 snap-start flex-col rounded-3xl sm:w-80 xl:w-auto xl:min-w-72 xl:max-w-none xl:flex-1',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardColumnComponent {
  column = input.required<BoardColumn>();
  showDates = input(false);
  loading = input(false);

  dropped = output<CdkDragDrop<AppointmentSummaryItem[]>>();
  open = output<AppointmentSummaryItem>();
}
