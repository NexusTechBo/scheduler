import { DialogRef } from '@angular/cdk/dialog';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, filter, merge } from 'rxjs';
import { SpecialtyLabelPipe } from '../../pipes/specialty-label.pipe';
import { TimeRangePipe } from '../../pipes/time-range.pipe';
import { DialogFrameComponent } from '../../ui/dialog-frame/dialog-frame.component';
import { LoadErrorComponent } from '../../ui/load-error/load-error.component';
import { SkeletonComponent } from '../../ui/skeleton/skeleton.component';
import { StatusBadgeComponent } from '../../ui/status-badge/status-badge.component';
import { UiIconComponent } from '../../ui/ui-icon/ui-icon.component';
import { AppointmentDetailService } from './appointment-detail.service';

export interface AppointmentDetailData {
  id: number;
}

@Component({
  selector: 'app-appointment-detail-dialog',
  imports: [
    LoadErrorComponent,
    DialogFrameComponent,
    SkeletonComponent,
    StatusBadgeComponent,
    UiIconComponent,
    TimeRangePipe,
    SpecialtyLabelPipe,
  ],
  providers: [AppointmentDetailService],
  templateUrl: './appointment-detail-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentDetailDialogComponent {
  protected readonly detail = inject(AppointmentDetailService);
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);

  constructor() {
    merge(
      this.ref.backdropClick.pipe(filter((event) => event.detail <= 1)),
      this.ref.keydownEvents.pipe(filter((event) => event.key === 'Escape')),
    )
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.close());
  }

  protected confirm(event: Event): void {
    this.apply(this.detail.confirm(), event);
  }

  protected cancel(event: Event): void {
    this.apply(this.detail.cancel(), event);
  }

  protected close(): void {
    this.ref.close(this.detail.changed);
  }

  private apply(action: Observable<unknown>, event: Event): void {
    const trigger = event.currentTarget as HTMLElement;
    action.subscribe({
      complete: () =>
        afterNextRender(
          () =>
            (trigger.isConnected
              ? trigger
              : this.host.nativeElement.querySelector<HTMLElement>('h2')
            )?.focus(),
          { injector: this.injector },
        ),
    });
  }
}
