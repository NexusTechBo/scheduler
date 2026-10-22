import { ElementRef, afterRenderEffect, inject, signal } from '@angular/core';

export type FocusTarget = string | (() => HTMLElement | null | undefined);

export function focusAfterLoad(busy: () => boolean): (...targets: FocusTarget[]) => void {
  const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  const pending = signal<FocusTarget[] | null>(null);
  afterRenderEffect(() => {
    const targets = pending();
    if (!targets || busy()) return;
    const target = [...targets, 'h1']
      .map((t) => (typeof t === 'string' ? host.querySelector<HTMLElement>(t) : t()))
      .find(Boolean);
    target?.focus();
    pending.set(null);
  });
  return (...targets) => pending.set(targets);
}
