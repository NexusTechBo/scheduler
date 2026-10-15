import { Injectable, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable, from, map, shareReplay } from 'rxjs';
import { ICON_REGISTRY, IconName } from './icon-registry';

/**
 * Fetches and caches raw SVG markup by registry name.
 * Uses the native fetch API (not HttpClient) so icon requests skip the app's
 * HTTP interceptors — icons are static assets, not API calls.
 */
@Injectable({ providedIn: 'root' })
export class IconLoaderService {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly cache = new Map<IconName, Observable<SafeHtml>>();

  load(name: IconName): Observable<SafeHtml> {
    const cached = this.cache.get(name);
    if (cached) return cached;

    const icon$ = from(fetch(ICON_REGISTRY[name]).then((response) => response.text())).pipe(
      map((svg) => this.sanitizer.bypassSecurityTrustHtml(svg)),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.cache.set(name, icon$);
    return icon$;
  }
}
