import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

/**
 * Global HTTP Error Interceptor
 * API'den dönen hataları yakalar ve ToastService aracılığıyla kullanıcıya gösterir.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 429 Too Many Requests (Rate Limiter)
      if (error.status === 429) {
        toastService.error('Çok fazla işlem yaptınız. Lütfen biraz bekleyip tekrar deneyiniz.', 7000);
      } 
      // 500 Internal Server Error (opsiyonel)
      else if (error.status >= 500) {
        toastService.error('Sunucuda beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyiniz.');
      }
      
      // Hatanın çağrıldığı yere (component/service) ilerleyebilmesi için fırlatıyoruz:
      return throwError(() => error);
    })
  );
};
