import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { LoadErrorComponent } from '../../shared/ui/load-error/load-error.component';
import { FormFieldComponent } from '../../shared/ui/forms/form-field/form-field.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';
import { SpecialtiesService } from './services/specialties.service';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';

@Component({
  selector: 'app-specialties-page',
  imports: [
    SkeletonComponent,
    FormsModule,
    FormFieldComponent,
    PageHeaderComponent,
    EmptyStateComponent,
    LoadErrorComponent,
    UiIconComponent,
  ],
  providers: [SpecialtiesService],
  templateUrl: './specialties-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpecialtiesPageComponent {
  protected readonly specialties = inject(SpecialtiesService);
  protected readonly name = signal('');

  protected create(): void {
    const name = this.name().trim();
    if (!name || this.specialties.saving()) return;
    this.specialties.create(name).subscribe(() => this.name.set(''));
  }
}
