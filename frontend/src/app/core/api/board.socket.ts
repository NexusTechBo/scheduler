import { DestroyRef, Injectable, NgZone, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IMessage, ReconnectionTimeMode, RxStomp } from '@stomp/rx-stomp';
import { Observable, defer } from 'rxjs';
import { AuthService } from '../auth/auth.service';

function wsUrl(path: string): string {
  const url = new URL(path, location.href);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}

@Injectable()
export class BoardSocket {
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);
  private readonly stomp = new RxStomp();

  readonly connected$: Observable<unknown> = this.stomp.connected$;

  readonly events$: Observable<IMessage> = defer(() =>
    this.stomp.watch({ destination: this.boardTopic() }),
  );

  constructor() {
    let connectionToken: string | null = null;
    this.stomp.configure({
      brokerURL: wsUrl('/ws'),
      beforeConnect: (stomp) => {
        connectionToken = this.auth.token();
        stomp.configure({ connectHeaders: { Authorization: `Bearer ${connectionToken ?? ''}` } });
      },
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      reconnectDelay: 1_000,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      maxReconnectDelay: 30_000,
    });

    this.stomp.stompErrors$.pipe(takeUntilDestroyed()).subscribe(() =>
      this.zone.run(() => {
        if (connectionToken) this.auth.expireSession(connectionToken);
      }),
    );

    this.zone.runOutsideAngular(() => this.stomp.activate());
    inject(DestroyRef).onDestroy(() => void this.stomp.deactivate());
  }

  isConnected(): boolean {
    return this.stomp.connected();
  }

  private boardTopic(): string {
    const clinicTopic = `/topic/clinic/${this.auth.session()?.clinicId}`;
    return this.auth.restrictedToOwnData()
      ? `${clinicTopic}/doctor/${this.auth.userId()}/board`
      : `${clinicTopic}/board`;
  }
}
