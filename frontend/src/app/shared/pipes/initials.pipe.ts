import { Pipe, PipeTransform } from '@angular/core';
import { initials } from '../../core/utils/labels.util';

@Pipe({ name: 'initials' })
export class InitialsPipe implements PipeTransform {
  transform(name: string | null | undefined): string {
    return initials(name);
  }
}
