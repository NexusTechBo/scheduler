import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop, CdkDropListGroup, moveItemInArray } from '@angular/cdk/drag-drop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { BoardSocket } from '../../core/api/board.socket';
import { AuthService } from '../../core/auth/auth.service';
import { canManageAppointments } from '../../core/auth/permissions';
import { AppointmentStatus, AppointmentSummaryItem } from '../../core/models/api.models';
import { DateRangePickerComponent } from '../../shared/ui/forms/date-range-picker/date-range-picker.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { BoardService } from './services/board.service';
import { BoardColumnComponent } from './components/board-column/board-column.component';
import { AlertComponent } from '../../shared/ui/alert/alert.component';

@Component({
  selector: 'app-board',
  imports: [
    AlertComponent,
    CdkDropListGroup,
    ReactiveFormsModule,
    BoardColumnComponent,
    PageHeaderComponent,
    DateRangePickerComponent,
    UiIconComponent,
  ],
  providers: [BoardService, BoardSocket],
  templateUrl: './board.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardComponent {
  protected readonly board = inject(BoardService);
  protected readonly readonly = !canManageAppointments(inject(AuthService).role());
  protected readonly rangeControl = new FormControl(this.board.range(), { nonNullable: true });

  constructor() {
    this.rangeControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((range) => this.board.range.set(range));
  }

  protected drop(event: CdkDragDrop<AppointmentSummaryItem[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }
    this.board.move(event.item.data, event.container.id as AppointmentStatus);
  }
}
