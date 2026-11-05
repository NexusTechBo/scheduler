import { DestroyRef } from '@angular/core';
import { Cooldown, createCooldown } from './cooldown.util';

class FakeDestroyRef implements DestroyRef {
  private callbacks: (() => void)[] = [];
  destroyed = false;
  onDestroy(callback: () => void): () => void {
    this.callbacks.push(callback);
    return () => undefined;
  }
  destroy(): void {
    this.callbacks.forEach((callback) => callback());
  }
}

describe('createCooldown', () => {
  let destroyRef: FakeDestroyRef;
  let cooldown: Cooldown;

  beforeEach(() => {
    vi.useFakeTimers();
    destroyRef = new FakeDestroyRef();
    cooldown = createCooldown(destroyRef);
  });

  afterEach(() => vi.useRealTimers());

  it('is inactive initially', () => {
    expect(cooldown.active()).toBe(false);
    expect(cooldown.label()).toBe('');
    expect(cooldown.finished()).toBe(false);
  });

  it('counts down every second and finishes', () => {
    const onDone = vi.fn();
    cooldown.start(2, onDone);
    expect(cooldown.active()).toBe(true);
    expect(cooldown.label()).toBe('2 s');

    vi.advanceTimersByTime(1000);
    expect(cooldown.label()).toBe('1 s');

    vi.advanceTimersByTime(1000);
    expect(cooldown.active()).toBe(false);
    expect(cooldown.finished()).toBe(true);
    expect(onDone).toHaveBeenCalledOnce();
  });

  it('formats long waits as minutes', () => {
    cooldown.start(125);
    expect(cooldown.label()).toBe('2:05 min');
  });

  it('does nothing for a zero or negative duration', () => {
    cooldown.start(0);
    cooldown.start(-3);
    expect(cooldown.active()).toBe(false);
    expect(cooldown.finished()).toBe(false);
  });

  it('restarts when started again', () => {
    cooldown.start(5);
    vi.advanceTimersByTime(4000);
    cooldown.start(3);
    expect(cooldown.label()).toBe('3 s');
  });

  it('resets the finished flag', () => {
    cooldown.start(1);
    vi.advanceTimersByTime(1000);
    cooldown.reset();
    expect(cooldown.finished()).toBe(false);
  });

  it('stops counting when destroyed', () => {
    cooldown.start(3);
    destroyRef.destroy();
    vi.advanceTimersByTime(5000);
    expect(cooldown.label()).toBe('3 s');
    expect(vi.getTimerCount()).toBe(0);
  });
});
