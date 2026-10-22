import { ElementRef, afterRenderEffect, inject, signal } from '@angular/core';

export function focusAfterLoad(busy: () => boolean): (...selectors: string[]) => void {
  const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  const pending = signal<string[] | null>(null);
  afterRenderEffect(() => {
    const selectors = pending();
    if (!selectors || busy()) return;
    const target = [...selectors, 'h1']
      .map((selector) => host.querySelector<HTMLElement>(selector))
      .find(Boolean);
    target?.focus();
    pending.set(null);
  });
  return (...selectors) => pending.set(selectors);
}
