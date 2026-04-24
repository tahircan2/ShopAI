import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Seller (Corporate) dashboard istatistik servisi.
 * Tüm istekler JWT cookie ile gönderilir — userId backend'de otomatik alınır.
 */
@Injectable({ providedIn: 'root' })
export class SellerStatsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/seller/stats`;

  getRevenue(days = 30): Observable<{ totalRevenue: number; change: number }> {
    return this.http.get<any>(`${this.base}/revenue`, { params: { days } });
  }

  getOrders(): Observable<{ totalOrders: number; ordersThisMonth: number; change: number }> {
    return this.http.get<any>(`${this.base}/orders`);
  }

  getCustomerCount(): Observable<{ totalCustomers: number; newThisMonth: number }> {
    return this.http.get<any>(`${this.base}/customers`);
  }

  getAvgRating(): Observable<{ avgRating: number; change: number }> {
    return this.http.get<any>(`${this.base}/avg-rating`);
  }

  getRevenueChart(days = 7): Observable<{ date: string; revenue: number }[]> {
    return this.http.get<any>(`${this.base}/revenue-chart`, { params: { days } });
  }

  getCategoryChart(): Observable<{ category: string; revenue: number; percentage: number }[]> {
    return this.http.get<any>(`${this.base}/category-chart`);
  }

  getRecentOrders(limit = 10): Observable<any[]> {
    return this.http.get<any>(`${this.base}/recent-orders`, { params: { limit } });
  }

  getTopProducts(limit = 5): Observable<any[]> {
    return this.http.get<any>(`${this.base}/top-products`, { params: { limit } });
  }
}
