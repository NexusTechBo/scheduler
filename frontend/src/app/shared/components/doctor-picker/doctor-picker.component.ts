import {
  ChangeDetectionStrategy,
  Component,
  booleanAttribute,
  computed,
  input,
  output,
} from '@angular/core';
import { InitialsPipe } from '../../pipes/initials.pipe';
import { SpecialtyLabelPipe } from '../../pipes/specialty-label.pipe';
import { LoadErrorComponent } from '../../ui/load-error/load-error.component';
import { SkeletonComponent } from '../../ui/skeleton/skeleton.component';

export interface DoctorCardItem {
  id: number;
  name: string;
  specialtyName: string | null;
}

@Component({
  selector: 'app-doctor-picker',
  imports: [InitialsPipe, LoadErrorComponent, SkeletonComponent, SpecialtyLabelPipe],
  templateUrl: './doctor-picker.component.html',
  host: { class: 'block', '[attr.aria-busy]': 'loading()' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DoctorPickerComponent<T extends DoctorCardItem> {
  readonly doctors = input.required<readonly T[]>();
  readonly selectedId = input<number | null | undefined>(null);
  readonly loading = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly emptyMessage = input('No hay doctores disponibles.');
  readonly wide = input(false, { transform: booleanAttribute });
  readonly select = output<T>();
  readonly retry = output<void>();

  protected readonly gridClass = computed(() =>
    this.wide() ? 'grid gap-2.5 sm:grid-cols-2 2xl:grid-cols-3' : 'grid gap-2.5 sm:grid-cols-2',
  );
}
