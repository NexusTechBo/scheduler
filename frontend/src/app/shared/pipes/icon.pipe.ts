import { Pipe, PipeTransform, inject } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { Observable } from 'rxjs';
import { IconLoaderService } from '../../core/icons/icon-loader.service';
import { IconName } from '../../core/icons/icon-registry';

@Pipe({ name: 'icon' })
export class IconPipe implements PipeTransform {
  private readonly iconLoader = inject(IconLoaderService);

  transform(name: IconName): Observable<SafeHtml> {
    return this.iconLoader.load(name);
  }
}
