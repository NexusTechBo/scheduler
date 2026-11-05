import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = TestBed.inject(NotificationService);
  });

  afterEach(() => vi.useRealTimers());

  it('queues success and error notifications in order', () => {
    service.success('Guardado');
    service.error('Falló');
    expect(service.notifications().map((n) => [n.type, n.message])).toEqual([
      ['success', 'Guardado'],
      ['error', 'Falló'],
    ]);
  });

  it('assigns unique ids', () => {
    service.success('a');
    service.success('b');
    const [first, second] = service.notifications();
    expect(first.id).not.toBe(second.id);
  });

  it('dismisses a notification by id', () => {
    service.success('a');
    service.error('b');
    service.dismiss(service.notifications()[0].id);
    expect(service.notifications().map((n) => n.message)).toEqual(['b']);
  });

  it('auto dismisses after 4 seconds', () => {
    service.success('a');
    vi.advanceTimersByTime(3999);
    expect(service.notifications()).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(service.notifications()).toHaveLength(0);
  });
});
