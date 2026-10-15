import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { CatalogsApi } from '../../core/api/catalogs.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { NotificationService } from '../../shared/services/notification.service';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { SpecialtiesResourceService } from './services/specialties-resource.service';

@Component({
  selector: 'app-specialties-page',
  imports: [FormsModule, PageHeaderComponent, EmptyStateComponent, UiIconComponent],
  providers: [SpecialtiesResourceService],
  templateUrl: './specialties-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class SpecialtiesPageComponent {
  private readonly api = inject(CatalogsApi);
  private readonly notifications = inject(NotificationService);
  protected readonly resource = inject(SpecialtiesResourceService);

  protected readonly name = signal('');
  protected readonly saving = signal(false);

  protected create(): void {
    const name = this.name().trim();
    if (!name || this.saving()) return;
    this.saving.set(true);
    this.api
      .createSpecialty(name)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success(`Especialidad "${name}" creada`);
          this.name.set('');
          this.resource.reloadSpecialties();
        },
        error: (error) =>
          this.notifications.error(apiErrorMessage(error, 'No se pudo crear la especialidad')),
      });
  }
}
