import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Specialty } from '../../../core/models/api.models';

@Component({
  selector: 'app-specialty-filter',
  templateUrl: './specialty-filter.component.html',
  host: { class: 'flex flex-wrap gap-2', role: 'group', 'aria-label': 'Especialidad' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpecialtyFilterComponent {
  readonly specialties = input.required<readonly Specialty[]>();
  readonly selectedId = input<number | null>(null);
  readonly select = output<number | null>();
}
