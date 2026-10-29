import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { ClinicsApi } from '../../core/api/clinics.api';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { ClinicActivatedResponse } from '../../core/models/api.models';

export type ActivationState =
  | { status: 'loading' }
  | { status: 'success'; result: ClinicActivatedResponse }
  | { status: 'invalid' }
  | { status: 'error'; message: string };

@Injectable()
export class ActivationService {
  private readonly clinicsApi = inject(ClinicsApi);
  private readonly auth = inject(AuthService);

  private readonly _state = signal<ActivationState>({ status: 'loading' });
  readonly state = this._state.asReadonly();
  readonly status = computed(() => this._state().status);
  readonly result = computed(() => {
    const state = this._state();
    return state.status === 'success' ? state.result : null;
  });
  readonly errorMessage = computed(() => {
    const state = this._state();
    return state.status === 'error' ? state.message : null;
  });

  readonly hasSession = computed(() => {
    const session = this.auth.session();
    return session !== null && session.expiresAt > Date.now();
  });

  private token: string | null = null;
  private started = false;

  continueToLogin(): void {
    const username = this.result()?.adminUsername;
    if (!username) return;
    this.auth.logout({ state: { username } });
  }

  activate(token: string | null): void {
    if (this.started) return;
    this.started = true;
    this.token = token?.trim() || null;
    this.send();
  }

  retry(): void {
    if (this._state().status !== 'error') return;
    this.send();
  }

  private send(): void {
    const token = this.token;
    if (!token) {
      this._state.set({ status: 'invalid' });
      return;
    }
    this._state.set({ status: 'loading' });
    this.clinicsApi.activate(token).subscribe({
      next: (result) => {
        this.token = null;
        this._state.set({ status: 'success', result });
      },
      error: (error) => {
        if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.BadRequest) {
          this.token = null;
          this._state.set({ status: 'invalid' });
          return;
        }
        this._state.set({
          status: 'error',
          message: apiErrorMessage(error, 'No se pudo activar la clínica, intenta de nuevo'),
        });
      },
    });
  }
}
