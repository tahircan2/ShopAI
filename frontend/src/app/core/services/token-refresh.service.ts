import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * TokenRefreshService — Token yenileme durumunu yönetir.
 *
 * auth.interceptor.ts içindeki module-level mutable değişkenler
 * (`let isRefreshing`, `refreshSubject`) bu servis içine taşındı.
 *
 * Neden? Module-level değişkenler:
 * 1. Angular DI sisteminin dışında — test edilemez
 * 2. Lazy module yüklemelerinde state corruption'a açık
 * 3. Singleton davranışı garanti altında değil
 *
 * Bu servis `providedIn: 'root'` ile Angular singleton garantisi verir.
 */
@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private _isRefreshing = false;
  private _refreshSubject = new BehaviorSubject<boolean | null>(null);

  get isRefreshing(): boolean {
    return this._isRefreshing;
  }

  get refreshSubject(): BehaviorSubject<boolean | null> {
    return this._refreshSubject;
  }

  startRefresh(): void {
    this._isRefreshing = true;
    this._refreshSubject.next(null);
  }

  markRefreshSuccess(): void {
    this._isRefreshing = false;
    this._refreshSubject.next(true);
  }

  markRefreshFailed(): void {
    this._isRefreshing = false;
    this._refreshSubject.next(false);
  }
}
