import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Skip loading indicator for silent requests (e.g., session check)
  if (req.headers.has('X-Silent')) {
    return next(req.clone({ headers: req.headers.delete('X-Silent') }));
  }

  loadingService.start();
  return next(req).pipe(finalize(() => loadingService.stop()));
};
