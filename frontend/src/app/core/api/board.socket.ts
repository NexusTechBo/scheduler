import { Injectable, NgZone, effect, inject } from '@angular/core';
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
    this.stomp.watch({ destination: `/topic/clinic/${this.auth.session()?.clinicId}/board` }),
  );

  constructor() {
    this.stomp.configure({
      brokerURL: wsUrl('/ws'),
      beforeConnect: (stomp) =>
        stomp.configure({ connectHeaders: { Authorization: `Bearer ${this.auth.token() ?? ''}` } }),
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      reconnectDelay: 1_000,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      maxReconnectDelay: 30_000,
    });

    this.stomp.stompErrors$.subscribe(() => this.zone.run(() => this.auth.logout()));

    effect(() => {
      if (this.auth.isAuthenticated()) this.zone.runOutsideAngular(() => this.stomp.activate());
      else void this.stomp.deactivate();
    });
  }
}
