import { HttpInterceptorFn } from '@angular/common/http';

const MUTATION_METHODS = ['POST', 'PUT', 'DELETE', 'PATCH'];

function getCookieValue(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(^|;\\s*)${name}=([^;]*)`));
  return match ? decodeURIComponent(match[2]) : null;
}

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (MUTATION_METHODS.includes(req.method.toUpperCase())) {
    const token = getCookieValue('XSRF-TOKEN');
    if (token) {
      const updated = req.clone({ setHeaders: { 'X-XSRF-TOKEN': token } });
      return next(updated);
    }
  }
  return next(req);
};
