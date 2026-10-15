import { Component, inject } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { FormsModule } from '@angular/forms';
import { filter, switchMap } from 'rxjs';
import { StaffApi } from '../../core/api/staff.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { ROLE_ID, Staff } from '../../core/models/api.models';
import { ROLE_LABEL, initials } from '../../core/utils/labels.util';
import { NotificationService } from '../../shared/services/notification.service';
import { ConfirmService } from '../../shared/ui/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import { SegmentedTabItem, SegmentedTabsComponent } from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { StaffFormData, StaffFormDialogComponent } from './staff-form-dialog.component';
import { StaffResourceService } from './services/staff-resource.service';

/** ADMINISTRATOR: doctors and assistants of the clinic (backend `/api/personal`). */
@Component({
  selector: 'app-staff-page',
  imports: [FormsModule, PageHeaderComponent, SegmentedTabsComponent, PaginationComponent, EmptyStateComponent, UiIconComponent],
  providers: [StaffResourceService],
  templateUrl: './staff-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' }
})
export class StaffPageComponent {
  private readonly api = inject(StaffApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(Dialog);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);
  protected readonly resource = inject(StaffResourceService);

  protected readonly roleLabel = ROLE_LABEL;
  protected readonly initials = initials;
  protected readonly myId = this.auth.userId();
  protected readonly tabs: SegmentedTabItem[] = [
    { id: 'all', label: 'Todos' },
    { id: String(ROLE_ID.DOCTOR), label: 'Doctores' },
    { id: String(ROLE_ID.ASSISTANT), label: 'Asistentes' }
  ];

  protected openForm(staff?: Staff): void {
    this.dialog
      .open<Staff, StaffFormData>(StaffFormDialogComponent, { data: { staff }, backdropClass: 'glass-backdrop' })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success(staff ? 'Datos actualizados' : 'Miembro del personal creado');
        this.resource.reloadStaff();
      });
  }

  protected deactivate(staff: Staff): void {
    this.confirm
      .ask({
        title: 'Desactivar cuenta',
        message: `${staff.name} dejará de aparecer para nuevas reservas. Sus citas existentes se mantienen.`,
        confirmLabel: 'Desactivar',
        danger: true
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deactivate(staff.id))
      )
      .subscribe({
        next: () => {
          this.notifications.success('Cuenta desactivada');
          this.resource.reloadStaff();
        },
        error: (error) => this.notifications.error(apiErrorMessage(error))
      });
  }
}
