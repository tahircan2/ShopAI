import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order, OrderSummary, CreateOrderRequest } from '../models/product.model';
import { User, AddressRequest, AddressResponse, Notification } from '../models/user.model';

// ─── Order Service ───────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/orders`;

  // ... inside OrderService ...
  readonly orders = signal<OrderSummary[]>([]);

  createOrder(req: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.api, req);
  }

  getOrderByNumber(orderNumber: string): Observable<Order> {
    return this.http.get<Order>(`${this.api}/${orderNumber}`);
  }

  getMyOrders(page = 0, size = 10): Observable<{ content: OrderSummary[]; totalElements: number }> {
    return this.http.get<{ content: OrderSummary[]; totalElements: number }>(`${environment.apiUrl}/users/me/orders`, {
      params: { page, size }
    }).pipe(tap(res => this.orders.set(res.content)));
  }

  cancelOrder(orderNumber: string): Observable<Order> {
    return this.http.post<Order>(`${this.api}/${orderNumber}/cancel`, {});
  }

  // Admin
  getAllOrders(page = 0, size = 20): Observable<{ content: OrderSummary[]; totalElements: number }> {
    return this.http.get<{ content: OrderSummary[]; totalElements: number }>(`${environment.apiUrl}/admin/orders`, {
      params: { page, size }
    });
  }

  updateOrderStatus(id: number, status: string): Observable<Order> {
    return this.http.put<Order>(`${environment.apiUrl}/admin/orders/${id}/status`, { status });
  }
}
