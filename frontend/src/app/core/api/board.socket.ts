import { Injectable, NgZone, effect, inject } from '@angular/core';
import { IMessage, ReconnectionTimeMode, RxStomp } from '@stomp/rx-stomp';
import { Observable, defer } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Relative paths become ws(s)://<current host>/... so the dev proxy and prod nginx both work. */
function wsUrl(path: string): string {
  const url = new URL(path, location.href);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  return url.toString();
}

/**
 * STOMP connection to the backend (`/ws`). Connected only while logged in; the JWT goes in the
 * CONNECT frame and is re-read on every (re)connect.
 */
@Injectable()
export class BoardSocket {
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);
  private readonly stomp = new RxStomp();

  /** Emits every time the connection is (re)established — consumers resync from REST. */
  readonly connected$: Observable<unknown> = this.stomp.connected$;

  /** Board changes of the current user's clinic (body is ignored: consumers reload from REST). */
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

    // ERROR frame = server rejected the CONNECT (bad/expired token): log out instead of retrying forever.
    this.stomp.stompErrors$.subscribe(() => this.zone.run(() => this.auth.logout()));

    // Side effect leaving Angular: open on login, close on logout / token expiry.
    // Outside the zone so heartbeats don't trigger app-wide change detection.
    effect(() => {
      if (this.auth.isAuthenticated()) this.zone.runOutsideAngular(() => this.stomp.activate());
      else void this.stomp.deactivate();
    });
  }
}
