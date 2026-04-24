import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * User personal stats service.
 * Tüm istekler JWT cookie ile gönderilir — userId backend'de otomatik alınır.
 */
@Injectable({ providedIn: 'root' })
export class UserStatsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/user/stats`;

  getPersonalStats(): Observable<{
    totalSpend: number;
    totalOrders: number;
    avgOrderValue: number;
    membership: string;
    membershipProgress: number;
    nextThreshold: number;
  }> {
    return this.http.get<any>(`${this.base}/personal`);
  }

  getMonthlySpend(): Observable<{ year: number; month: number; spend: number }[]> {
    return this.http.get<any>(`${this.base}/monthly-spend`);
  }

  getCategorySpend(): Observable<{ category: string; spend: number; percentage: number }[]> {
    return this.http.get<any>(`${this.base}/category-spend`);
  }

  getRecentOrders(limit = 10): Observable<any[]> {
    return this.http.get<any>(`${this.base}/recent-orders`, { params: { limit } });
  }
}
