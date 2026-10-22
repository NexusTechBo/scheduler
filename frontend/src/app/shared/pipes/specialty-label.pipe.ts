import { Pipe, PipeTransform } from '@angular/core';

export function specialtyLabel(name: string | null | undefined): string {
  return name ?? 'Sin especialidad';
}

@Pipe({ name: 'specialtyLabel' })
export class SpecialtyLabelPipe implements PipeTransform {
  transform(name: string | null | undefined): string {
    return specialtyLabel(name);
  }
}
