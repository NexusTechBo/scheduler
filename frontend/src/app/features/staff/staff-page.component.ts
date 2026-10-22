import { ChangeDetectionStrategy, Component, Injector, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { labelledByFrame } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { Staff } from '../../core/models/api.models';
import { ROLE_LABEL, initials } from '../../core/utils/labels.util';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import {
  SegmentedTabItem,
  SegmentedTabsComponent,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import {
  StaffFormData,
  StaffFormDialogComponent,
} from './components/staff-form-dialog/staff-form-dialog.component';
import { StaffRoleFilter, StaffService } from './services/staff.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { focusAfterLoad } from '../../shared/ui/focus-after-load';

@Component({
  selector: 'app-staff-page',
  imports: [
    SkeletonComponent,
    FormsModule,
    PageHeaderComponent,
    SegmentedTabsComponent,
    PaginationComponent,
    EmptyStateComponent,
    UiIconComponent,
  ],
  providers: [StaffService],
  templateUrl: './staff-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  protected readonly staffService = inject(StaffService);
  private readonly focusRow = focusAfterLoad(this.staffService.isLoading);

  protected readonly roleLabel = ROLE_LABEL;
  protected readonly initials = initials;
  protected readonly myId = inject(AuthService).userId();
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'all', label: 'Todos' },
    { id: 'DOCTOR', label: 'Doctores' },
    { id: 'ASSISTANT', label: 'Asistentes' },
  ];

  protected setRole(role: string): void {
    this.staffService.setRole(role as StaffRoleFilter);
  }

  protected deactivate(staff: Staff): void {
    this.staffService
      .deactivate(staff)
      .subscribe(() => this.focusRow(`#staff-edit-${staff.id}`, 'tbody button'));
  }

  protected openForm(staff?: Staff): void {
    this.dialog.open<void, StaffFormData>(StaffFormDialogComponent, {
      ...labelledByFrame(),
      data: { staff },
      injector: this.injector,
    });
  }
}
