import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Cart, AddToCartRequest, ApplyCouponRequest } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/cart`;

  readonly cart = signal<Cart | null>(null);
  readonly cartCount = computed(() => this.cart()?.items.reduce((s, i) => s + i.quantity, 0) ?? 0);
  readonly cartTotal = computed(() => this.cart()?.total ?? 0);

  getCart(): Observable<Cart> {
    return this.http.get<Cart>(this.api).pipe(tap(c => this.cart.set(c)));
  }

  addToCart(req: AddToCartRequest): Observable<Cart> {
    return this.http.post<Cart>(`${this.api}/items`, req).pipe(tap(c => this.cart.set(c)));
  }

  updateQuantity(itemId: number, quantity: number): Observable<Cart> {
    return this.http.put<Cart>(`${this.api}/items/${itemId}`, { quantity }).pipe(tap(c => this.cart.set(c)));
  }

  removeItem(itemId: number): Observable<Cart> {
    return this.http.delete<Cart>(`${this.api}/items/${itemId}`).pipe(tap(c => this.cart.set(c)));
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.api).pipe(tap(() => this.cart.set(null)));
  }

  applyCoupon(req: ApplyCouponRequest): Observable<Cart> {
    return this.http.post<Cart>(`${this.api}/coupon`, req).pipe(tap(c => this.cart.set(c)));
  }

  removeCoupon(): Observable<Cart> {
    return this.http.delete<Cart>(`${this.api}/coupon`).pipe(tap(c => this.cart.set(c)));
  }
}
