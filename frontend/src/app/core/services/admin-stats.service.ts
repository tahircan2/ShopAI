import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Admin platform-wide stats service.
 * Tüm istekler JWT cookie ile gönderilir.
 */
@Injectable({ providedIn: 'root' })
export class AdminStatsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/stats`;

  getPlatformRevenue(): Observable<{ totalRevenue: number; change: number }> {
    return this.http.get<any>(`${this.base}/platform-revenue`);
  }

  getUsersStats(): Observable<{ total: number; newThisMonth: number; byRole: Record<string, number> }> {
    return this.http.get<any>(`${this.base}/users`);
  }

  getStoresStats(): Observable<{ total: number; active: number; newThisMonth: number }> {
    return this.http.get<any>(`${this.base}/stores`);
  }

  getAov(): Observable<{ avgOrderValue: number }> {
    return this.http.get<any>(`${this.base}/aov`);
  }

  getPlatformRevenueChart(days = 30): Observable<{ date: string; revenue: number }[]> {
    return this.http.get<any>(`${this.base}/platform-revenue-chart`, { params: { days } });
  }

  getStoreComparison(): Observable<any[]> {
    return this.http.get<any>(`${this.base}/store-comparison`);
  }

  getUserRoleDistribution(): Observable<{ role: string; count: number; percentage: number }[]> {
    return this.http.get<any>(`${this.base}/user-role-distribution`);
  }

  getAuditLogs(limit = 20): Observable<any[]> {
    return this.http.get<any>(`${this.base}/audit-logs`, { params: { limit } });
  }
}
