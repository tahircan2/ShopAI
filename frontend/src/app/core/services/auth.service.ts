import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  User, AuthRequest, RegisterRequest, AuthResponse,
  SessionStatus, PasswordChangeRequest, ForgotPasswordRequest, ResetPasswordRequest
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = `${environment.apiUrl}/auth`;

  // ── Signals ──────────────────────────────────────────────────────────────
  readonly currentUser = signal<User | null>(null);
  readonly isLoading = signal(false);
  readonly sessionExpired = signal(false);

  // Computed
  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isSeller = computed(() => this.currentUser()?.role === 'SELLER');
  readonly userRole = computed(() => this.currentUser()?.role ?? null);
  readonly userFullName = computed(() => {
    const u = this.currentUser();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  // ── Auth Endpoints ────────────────────────────────────────────────────────

  login(req: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/login`, req).pipe(
      tap(res => {
        this.currentUser.set(res.user);
        this.sessionExpired.set(false);
      })
    );
  }

  register(req: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.api}/register`, req);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.api}/logout`, {}).pipe(
      tap(() => {
        this.currentUser.set(null);
        this.router.navigate(['/auth/login']);
      }),
      catchError(err => {
        // Even if backend fails, clear local state
        this.currentUser.set(null);
        this.router.navigate(['/auth/login']);
        return throwError(() => err);
      })
    );
  }

  refreshSession(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/refresh`, {}).pipe(
      tap(res => this.currentUser.set(res.user))
    );
  }

  checkSession(): Observable<SessionStatus> {
    return this.http.get<SessionStatus>(`${this.api}/me`).pipe(
      tap(status => {
        this.currentUser.set(status.loggedIn ? status.user : null);
      }),
      catchError(() => {
        this.currentUser.set(null);
        return throwError(() => new Error('Session check failed'));
      })
    );
  }

  forgotPassword(req: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.api}/forgot-password`, req);
  }

  resetPassword(req: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.api}/reset-password`, req);
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.get<void>(`${this.api}/verify-email`, { params: { token } });
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>(`${this.api}/resend-verification`, { email });
  }

  changePassword(req: PasswordChangeRequest): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/users/me/password`, req);
  }

  // ── Token Expiry Handler ──────────────────────────────────────────────────

  handleTokenExpiry(): void {
    this.sessionExpired.set(true);
    this.currentUser.set(null);
  }
}
