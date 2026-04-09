import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

let isRefreshing = false;
const refreshSubject = new BehaviorSubject<boolean | null>(null);

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const toastService = inject(ToastService);

  // Add withCredentials to all requests so cookies are sent automatically
  const withCreds = req.clone({ withCredentials: true });

  return next(withCreds).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        const expired = err.error?.expired || err.error?.error === 'TOKEN_EXPIRED';

        // Don't refresh for login/register/refresh endpoints
        const isAuthEndpoint = req.url.includes('/auth/login') ||
          req.url.includes('/auth/register') ||
          req.url.includes('/auth/refresh');

        if (expired && !isAuthEndpoint) {
          return handleTokenRefresh(req, next, authService, toastService);
        }
      }

      if (err.status === 403) {
        toastService.error('Bu işlem için yetkiniz yok.');
      }

      return throwError(() => err);
    })
  );
};

function handleTokenRefresh(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  toastService: ToastService
) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshSubject.next(null);

    return authService.refreshSession().pipe(
      switchMap(() => {
        isRefreshing = false;
        refreshSubject.next(true);
        // Retry original request — cookie is auto-sent
        return next(req.clone({ withCredentials: true }));
      }),
      catchError(err => {
        isRefreshing = false;
        refreshSubject.next(false);
        // Refresh failed — session expired
        authService.handleTokenExpiry();
        return throwError(() => err);
      })
    );
  }

  // Queue pending requests until refresh completes
  return refreshSubject.pipe(
    filter(done => done !== null), // wait until boolean is emitted
    take(1),
    switchMap(success => {
      if (success) {
        return next(req.clone({ withCredentials: true }));
      } else {
        return throwError(() => new Error('Session expired.'));
      }
    })
  );
}
