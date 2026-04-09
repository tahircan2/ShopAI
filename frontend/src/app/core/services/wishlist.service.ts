import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WishlistItem, ProductSummary } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users/me/wishlist`;

  readonly items = signal<ProductSummary[]>([]);
  readonly wishlistIds = signal<Set<number>>(new Set());

  load() {
    return this.http.get<ProductSummary[]>(this.api).pipe(
      tap(items => {
        this.items.set(items);
        this.wishlistIds.set(new Set(items.map(i => i.id)));
      })
    );
  }

  add(productId: number) {
    return this.http.post<void>(`${this.api}/${productId}`, {}).pipe(
      tap(() => this.wishlistIds.update(ids => new Set([...ids, productId])))
    );
  }

  remove(productId: number) {
    return this.http.delete<void>(`${this.api}/${productId}`).pipe(
      tap(() => {
        this.items.update(its => its.filter(i => i.id !== productId));
        this.wishlistIds.update(ids => { const s = new Set(ids); s.delete(productId); return s; });
      })
    );
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistIds().has(productId);
  }
}
