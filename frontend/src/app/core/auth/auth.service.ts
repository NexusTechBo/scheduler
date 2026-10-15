import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { Observable, map, tap } from 'rxjs';
import { AuthApi } from '../api/auth.api';
import { Clinic, Role, TokenPayload } from '../models/api.models';

const TOKEN_KEY = 'scheduler.token';
const CLINIC_KEY = 'scheduler.clinic';

export interface Session {
  token: string;
  /** Personal.id for staff, Patient.id for patients — the id the backend uses for "self" checks. */
  userId: number;
  role: Role;
  clinicId: number;
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
  } catch {
    /* storage unavailable (private mode): the session just won't survive a reload */
  }
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
      username: payload.username,
      expiresAt: payload.exp * 1000
    };
    return session.expiresAt > Date.now() ? session : null;
  } catch {
    return null;
  }
}

/**
 * Single source of truth for who is logged in. The JWT is the session: there is no refresh endpoint,
 * tokens last 24h and are bound to one clinic (tenant).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);

  private readonly _session = signal<Session | null>(decodeSession(readStorage(TOKEN_KEY)));
  private readonly _clinic = signal<Clinic | null>(this.readClinic());

  readonly session = this._session.asReadonly();
  /** Clinic picked on /clinic-options (or the one the current session belongs to). */
  readonly clinic = this._clinic.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly role = computed(() => this._session()?.role ?? null);
  readonly userId = computed(() => this._session()?.userId ?? null);

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

  selectClinic(clinic: Clinic): void {
    this._clinic.set(clinic);
    writeStorage(CLINIC_KEY, JSON.stringify(clinic));
  }

  login(email: string, password: string, clinicId: number): Observable<Session> {
    return this.authApi.login({ email, password, clinicId }).pipe(
      map(({ token }) => {
        const session = decodeSession(token);
        if (!session) throw new Error('Token inválido');
        return session;
      }),
      tap((session) => {
        writeStorage(TOKEN_KEY, session.token);
        this._session.set(session);
      })
    );
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  /** Landing route after login, per role. */
  homeUrl(): string {
    return this.role() === 'PATIENT' ? '/appointments' : '/dashboard';
  }

  private clearSession(): void {
    writeStorage(TOKEN_KEY, null);
    this._session.set(null);
  }

  private readClinic(): Clinic | null {
    const raw = readStorage(CLINIC_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as Clinic;
    } catch {
      return null;
    }
  }
}
