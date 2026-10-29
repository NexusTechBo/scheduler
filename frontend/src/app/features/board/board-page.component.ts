import { CdkDragDrop, CdkDropListGroup, moveItemInArray } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BoardSocket } from '../../core/api/board.socket';
import { AppointmentStatus, AppointmentSummaryItem } from '../../core/models/api.models';
import { DateRangePickerComponent } from '../../shared/ui/forms/date-range-picker/date-range-picker.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { BoardService } from './board.service';
import { BoardColumnComponent } from './components/board-column/board-column.component';

@Component({
  selector: 'app-board-page',
  imports: [
    LoadErrorComponent,
    CdkDropListGroup,
    FormsModule,
    BoardColumnComponent,
    PageHeaderComponent,
    DateRangePickerComponent,
    UiIconComponent,
  ],
  providers: [BoardService, BoardSocket],
  templateUrl: './board-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardPageComponent {
  protected readonly board = inject(BoardService);

  protected drop(event: CdkDragDrop<AppointmentSummaryItem[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }
    this.board.move(event.item.data, event.container.id as AppointmentStatus);
  }
}
