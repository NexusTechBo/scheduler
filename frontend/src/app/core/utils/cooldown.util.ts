import { DestroyRef, Signal, computed, inject, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';

export interface Cooldown {
  readonly active: Signal<boolean>;
  readonly label: Signal<string>;
  readonly finished: Signal<boolean>;
  start(seconds: number, onDone?: () => void): void;
  reset(): void;
}

export function createCooldown(destroyRef = inject(DestroyRef)): Cooldown {
  const remaining = signal(0);
  const finished = signal(false);
  let countdown: Subscription | undefined;
  destroyRef.onDestroy(() => countdown?.unsubscribe());

  return {
    active: computed(() => remaining() > 0),
    label: computed(() => (remaining() > 0 ? formatCooldown(remaining()) : '')),
    finished: finished.asReadonly(),
    start(seconds: number, onDone?: () => void): void {
      countdown?.unsubscribe();
      const deadline = Date.now() + Math.max(0, seconds) * 1000;
      const secondsLeft = () => Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      remaining.set(secondsLeft());
      finished.set(false);
      if (!remaining()) return;
      countdown = interval(1000).subscribe(() => {
        remaining.set(secondsLeft());
        if (remaining()) return;
        countdown?.unsubscribe();
        finished.set(true);
        onDone?.();
      });
    },
    reset(): void {
      finished.set(false);
    },
  };
}

function formatCooldown(seconds: number): string {
  if (seconds < 60) return `${seconds} s`;
  const rest = String(seconds % 60).padStart(2, '0');
  return `${Math.floor(seconds / 60)}:${rest} min`;
}
