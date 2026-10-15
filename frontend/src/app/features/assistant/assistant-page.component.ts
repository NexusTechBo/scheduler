import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ChatApi } from '../../core/api/chat.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { UiIconComponent } from '../../shared/ui/ui-icon/ui-icon.component';

interface ChatMessage {
  from: 'user' | 'assistant';
  text: string;
  error?: boolean;
}

const STORAGE_PREFIX = 'scheduler.chat.';

/**
 * PATIENT chat with the scheduling agent (Spring AI + Gemini). The backend keeps the model memory per patient;
 * the transcript is mirrored in sessionStorage only so it survives navigation inside the tab.
 */
@Component({
  selector: 'app-assistant-page',
  imports: [FormsModule, RouterLink, PageHeaderComponent, UiIconComponent],
  templateUrl: './assistant-page.component.html',
  host: { class: 'flex min-h-0 flex-1 flex-col' }
})
export class AssistantPageComponent {
  private readonly chatApi = inject(ChatApi);
  private readonly auth = inject(AuthService);
  private readonly storageKey = `${STORAGE_PREFIX}${this.auth.session()?.clinicId}.${this.auth.userId()}`;
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  protected readonly suggestions = ['Quiero agendar una cita', '¿Qué especialidades tienen?', '¿Cuáles son mis próximas citas?'];
  protected readonly messages = signal<ChatMessage[]>(this.restore());
  protected readonly draft = signal('');
  protected readonly sending = signal(false);

  constructor() {
    effect(() => {
      const messages = this.messages();
      try {
        sessionStorage.setItem(this.storageKey, JSON.stringify(messages));
      } catch {
        /* storage unavailable: transcript just won't persist */
      }
      queueMicrotask(() => {
        const element = this.scroller()?.nativeElement;
        element?.scrollTo({ top: element.scrollHeight, behavior: 'smooth' });
      });
    });
  }

  protected send(text = this.draft()): void {
    const message = text.trim();
    if (!message || this.sending()) return;

    this.messages.update((list) => [...list, { from: 'user', text: message }]);
    this.draft.set('');
    this.sending.set(true);
    this.chatApi
      .send(message)
      .pipe(finalize(() => this.sending.set(false)))
      .subscribe({
        next: (reply) => this.messages.update((list) => [...list, { from: 'assistant', text: reply }]),
        error: (error) =>
          this.messages.update((list) => [
            ...list,
            {
              from: 'assistant',
              text: apiErrorMessage(error, 'El asistente no está disponible en este momento. Intenta de nuevo.'),
              error: true
            }
          ])
      });
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected clear(): void {
    this.messages.set([]);
  }

  private restore(): ChatMessage[] {
    try {
      return JSON.parse(sessionStorage.getItem(this.storageKey) ?? '[]') as ChatMessage[];
    } catch {
      return [];
    }
  }
}
