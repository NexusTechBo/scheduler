import { DestroyRef, Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationExtras, Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { Observable, map } from 'rxjs';
import { AuthApi } from '../api/auth.api';
import { Role, Staff, TokenPayload } from '../models/api.models';
import { HOME_URL, SESSION_ACCESS, SessionAccess } from '../navigation/navigation';

const TOKEN_KEY = 'scheduler.token';
const MAX_TIMER_MS = 2_147_483_647;

interface Session {
  token: string;
  userId: number;
  role: Role;
  roles: Role[];
  clinicId: number;
  clinicName: string;
  clinicSlug: string;
  username: string;
  expiresAt: number;
}

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {}
}

function isExpired(session: Session): boolean {
  return session.expiresAt <= Date.now();
}

function decodeSession(token: string | null): Session | null {
  if (!token) return null;
  try {
    const payload = jwtDecode<TokenPayload>(token);
    const session: Session = {
      token,
      userId: Number(payload.sub),
      role: payload.role,
      roles: payload.roles?.length ? payload.roles : [payload.role],
      clinicId: Number(payload.clinicId),
      clinicName: payload.clinicName,
      clinicSlug: payload.clinicSlug,
      username: payload.username,
      expiresAt: payload.exp * 1000,
    };
    return isExpired(session) ? null : session;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);

  private readonly _session = signal<Session | null>(decodeSession(readStorage(TOKEN_KEY)));
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;

  readonly session = this._session.asReadonly();
  readonly role = computed(() => this._session()?.role ?? null);
  readonly roles = computed<readonly Role[]>(() => this._session()?.roles ?? []);
  readonly restrictedToOwnData = computed(
    () => this.hasRole('DOCTOR') && !this.hasRole('ADMINISTRATOR'),
  );
  readonly userId = computed(() => this._session()?.userId ?? null);

  private readonly me = rxResource({
    params: () => this.userId() ?? undefined,
    stream: () => this.authApi.me(),
  });
  readonly profile = computed(() => (this.me.hasValue() ? this.me.value() : undefined));
  readonly profileLoading = this.me.isLoading;
  readonly profileError = this.me.error;
  readonly displayName = computed(
    () => this.profile()?.name ?? this._session()?.username.split('@')[0] ?? '',
  );

  readonly clinic = computed(() => {
    const session = this._session();
    return session ? { name: session.clinicName, slug: session.clinicSlug } : null;
  });

  constructor() {
    this.scheduleExpiry(this._session());

    const onStorage = (event: StorageEvent) => this.onStorage(event);
    window.addEventListener('storage', onStorage);
    inject(DestroyRef).onDestroy(() => {
      window.removeEventListener('storage', onStorage);
      clearTimeout(this.expiryTimer);
    });
  }

  setProfile(profile: Staff): void {
    this.me.set(profile);
  }

  reloadProfile(): void {
    this.me.reload();
  }

  token(): string | null {
    return this._session()?.token ?? null;
  }

  isAuthenticated(): boolean {
    const session = this._session();
    return !!session && !isExpired(session);
  }

  hasRole(...roles: Role[]): boolean {
    const current = this.roles();
    return roles.some((role) => current.includes(role));
  }

  login(username: string, password: string): Observable<Session> {
    return this.authApi
      .login({ username, password })
      .pipe(map(({ token }) => this.replaceToken(token)));
  }

  replaceToken(token: string): Session {
    const session = decodeSession(token);
    if (!session) throw new Error('Token inválido');
    writeStorage(TOKEN_KEY, session.token);
    this.setSession(session);
    return session;
  }

  logout(extras: NavigationExtras = {}): void {
    this.clearSession();
    this.router.navigate(['/login'], extras).finally();
  }

  expireSession(token: string): void {
    if (this._session()?.token !== token) return;
    this.clearSession();
  }

  private setSession(session: Session | null): void {
    this._session.set(session);
    this.scheduleExpiry(session);
  }

  private clearSession(): void {
    writeStorage(TOKEN_KEY, null);
    this.setSession(null);
  }

  private scheduleExpiry(session: Session | null): void {
    clearTimeout(this.expiryTimer);
    this.expiryTimer = undefined;
    if (!session) return;
    const delay = Math.min(Math.max(session.expiresAt - Date.now(), 0), MAX_TIMER_MS);
    this.expiryTimer = this.zone.runOutsideAngular(() =>
      setTimeout(
        () =>
          this.zone.run(() => {
            if (isExpired(session)) this.expireSession(session.token);
            else this.scheduleExpiry(session);
          }),
        delay,
      ),
    );
  }

  private onStorage(event: StorageEvent): void {
    if (event.key !== TOKEN_KEY && event.key !== null) return;
    const current = this._session();
    const next = decodeSession(readStorage(TOKEN_KEY));
    if (current?.token === next?.token) return;

    const access = this.currentSessionAccess();
    if (next && current && access === 'required') {
      const sameUser = current.userId === next.userId && current.clinicId === next.clinicId;
      if (!sameUser) {
        location.assign(HOME_URL);
        return;
      }
    }
    this.setSession(next);
    if (next && access === 'guestOnly') this.router.navigateByUrl(HOME_URL).finally();
  }

  private currentSessionAccess(): SessionAccess | null {
    let route: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    let access: SessionAccess | null = null;
    while (route) {
      access = (route.data[SESSION_ACCESS] as SessionAccess | undefined) ?? access;
      route = route.firstChild;
    }
    return access;
  }
}
