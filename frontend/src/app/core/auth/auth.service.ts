import { Injectable, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { Observable, map, tap } from 'rxjs';
import { AuthApi } from '../api/auth.api';
import { Role, Staff, TokenPayload } from '../models/api.models';

const TOKEN_KEY = 'scheduler.token';

interface Session {
  token: string;
  userId: number;
  role: Role;
  clinicId: number;
  clinicName: string;
  clinicSubdomain: string;
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

function decodeSession(token: string | null): Session | null {
  if (!token) return null;
  try {
    const payload = jwtDecode<TokenPayload>(token);
    const session: Session = {
      token,
      userId: Number(payload.sub),
      role: payload.role,
      clinicId: Number(payload.clinicId),
      clinicName: payload.clinicName,
      clinicSubdomain: payload.clinicSubdomain,
      username: payload.username,
      expiresAt: payload.exp * 1000,
    };
    return session.expiresAt > Date.now() ? session : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);

  private readonly _session = signal<Session | null>(decodeSession(readStorage(TOKEN_KEY)));

  readonly session = this._session.asReadonly();
  readonly role = computed(() => this._session()?.role ?? null);
  readonly userId = computed(() => this._session()?.userId ?? null);

  private readonly me = rxResource({
    loader: () => this.authApi.me(),
  });
  readonly displayName = computed(
    () =>
      (this.me.hasValue() ? this.me.value().name : null) ??
      this._session()?.username.split('@')[0] ??
      '',
  );

  readonly clinic = computed(() => {
    const session = this._session();
    return session ? { name: session.clinicName, subdomain: session.clinicSubdomain } : null;
  });

  constructor() {
    writeStorage('scheduler.clinic', null);
    writeStorage('scheduler.roles', null);
  }

  setProfile(profile: Staff): void {
    this.me.set(profile);
  }

  token(): string | null {
    const session = this._session();
    if (session && session.expiresAt <= Date.now()) {
      this.clearSession();
      return null;
    }
    return session?.token ?? null;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  login(username: string, password: string): Observable<Session> {
    return this.authApi.login({ username, password }).pipe(
      map(({ token }) => {
        const session = decodeSession(token);
        if (!session) throw new Error('Token inválido');
        return session;
      }),
      tap((session) => {
        writeStorage(TOKEN_KEY, session.token);
        this._session.set(session);
      }),
    );
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']).finally();
  }

  private clearSession(): void {
    writeStorage(TOKEN_KEY, null);
    this._session.set(null);
  }
}
