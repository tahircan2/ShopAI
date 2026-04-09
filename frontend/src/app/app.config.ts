import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { csrfInterceptor } from './core/interceptors/csrf.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor';
import { AuthService } from './core/services/auth.service';
import { catchError, of } from 'rxjs';

export function initializeAuth(authService: AuthService) {
  return () => authService.checkSession().pipe(
    catchError(() => of(null))
  );
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withViewTransitions(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' })
    ),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor, csrfInterceptor, loadingInterceptor])
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    }
  ]
};
