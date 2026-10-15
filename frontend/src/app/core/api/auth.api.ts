import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginRequest, LoginResponse, ProfileResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('auth/login', request);
  }

  me(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>('auth/me');
  }
}
