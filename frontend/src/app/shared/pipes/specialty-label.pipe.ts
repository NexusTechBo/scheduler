import { Pipe, PipeTransform } from '@angular/core';
import { specialtyLabel } from '../../core/utils/labels.util';

@Pipe({ name: 'specialtyLabel' })
export class SpecialtyLabelPipe implements PipeTransform {
  transform(name: string | null | undefined): string {
    return specialtyLabel(name);
  }
}
