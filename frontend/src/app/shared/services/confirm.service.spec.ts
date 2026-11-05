import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ConfirmDialogComponent } from '../ui/confirm-dialog/confirm-dialog.component';
import { ConfirmService } from './confirm.service';
import { NotificationService } from './notification.service';

describe('ConfirmService', () => {
  let service: ConfirmService;
  let closed: Subject<unknown>;
  let dialog: { open: ReturnType<typeof vi.fn> };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };
  const options = { title: 'Quitar', message: '¿Seguro?' };

  beforeEach(() => {
    closed = new Subject();
    dialog = { open: vi.fn(() => ({ closed })) };
    notifications = { success: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: Dialog, useValue: dialog },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    service = TestBed.inject(ConfirmService);
  });

  function close(result: unknown): void {
    closed.next(result);
    closed.complete();
  }

  describe('ask', () => {
    it('opens the confirm dialog labelled by its frame with the options', () => {
      service.ask(options).subscribe();
      expect(dialog.open).toHaveBeenCalledWith(
        ConfirmDialogComponent,
        expect.objectContaining({
          data: options,
          ariaLabelledBy: expect.stringMatching(/-title$/),
        }),
      );
    });

    it.each([
      [true, true],
      [false, false],
      [undefined, false],
    ])('maps the dialog result %s to %s', (result, expected) => {
      const answers: boolean[] = [];
      service.ask(options).subscribe((answer) => answers.push(answer));
      close(result);
      expect(answers).toEqual([expected]);
    });
  });

  describe('confirmThen', () => {
    it('runs the action and notifies success when confirmed', () => {
      const action = vi.fn(() => of('done'));
      const values: string[] = [];

      service.confirmThen(options, action, 'Listo').subscribe((v) => values.push(v));
      close(true);

      expect(action).toHaveBeenCalledOnce();
      expect(values).toEqual(['done']);
      expect(notifications.success).toHaveBeenCalledWith('Listo');
    });

    it('does not run the action when declined', () => {
      const action = vi.fn(() => of('done'));
      service.confirmThen(options, action, 'Listo').subscribe();
      close(false);
      expect(action).not.toHaveBeenCalled();
      expect(notifications.success).not.toHaveBeenCalled();
    });

    it('notifies the API error and completes on failure', () => {
      const action = () =>
        throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'En uso' } }));
      const outcome = { completed: false, failed: false };

      service.confirmThen(options, action, 'Listo', 'No se pudo').subscribe({
        complete: () => (outcome.completed = true),
        error: () => (outcome.failed = true),
      });
      close(true);

      expect(notifications.error).toHaveBeenCalledWith('En uso');
      expect(outcome).toEqual({ completed: true, failed: false });
    });

    it('uses the fallback message for unknown errors', () => {
      service
        .confirmThen(options, () => throwError(() => new Error('x')), 'Listo', 'No se pudo')
        .subscribe();
      close(true);
      expect(notifications.error).toHaveBeenCalledWith('No se pudo');
    });
  });
});
