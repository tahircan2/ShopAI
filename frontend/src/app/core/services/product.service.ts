import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Product, ProductSummary, ProductFilter, ProductPage, Review, ReviewRequest, Category, Coupon, CouponRequest } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/products`;

  readonly aiFilteredProducts = signal<ProductSummary[] | null>(null);
  readonly categories = signal<Category[]>([]);

  getProducts(filter: ProductFilter = {}): Observable<ProductPage> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([k, v]) => {
      // Map frontend `rating` to backend `minRating`
      const key = k === 'rating' ? 'minRating' : k;
      if (v !== undefined && v !== null) {
        if (Array.isArray(v)) v.forEach(val => params = params.append(key, val));
        else params = params.set(key, String(v));
      }
    });
    return this.http.get<ProductPage>(this.api, { params });
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.api}/${id}`);
  }

  getProductBySlug(slug: string): Observable<Product> {
    return this.http.get<Product>(`${this.api}/slug/${slug}`);
  }

  getFeaturedProducts(): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${this.api}/featured`);
  }

  searchProducts(q: string, page = 0, size = 20): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${this.api}/search`, { params: { q, page, size } });
  }

  getReviews(productId: number, page = 0): Observable<{ content: Review[]; totalElements: number }> {
    return this.http.get<{ content: Review[]; totalElements: number }>(`${this.api}/${productId}/reviews`, { params: { page } });
  }

  submitReview(productId: number, req: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${this.api}/${productId}/reviews`, req);
  }

  deleteReview(productId: number, reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${productId}/reviews/${reviewId}`);
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${environment.apiUrl}/categories`).pipe(
      tap(cats => this.categories.set(cats))
    );
  }

  applyAiFilter(products: ProductSummary[]): void { this.aiFilteredProducts.set(products); }
  clearAiFilter(): void { this.aiFilteredProducts.set(null); }

  // Admin / Seller
  createProduct(data: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(`${environment.apiUrl}/admin/products`, data);
  }

  updateProduct(id: number, data: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${environment.apiUrl}/admin/products/${id}`, data);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/products/${id}`);
  }

  // Seller-specific
  getMyProducts(page = 0, size = 20): Observable<ProductPage> {
    return this.http.get<ProductPage>(`${environment.apiUrl}/seller/products`, { params: { page, size } });
  }

  // Coupons
  getCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${environment.apiUrl}/admin/coupons`);
  }

  createCoupon(req: CouponRequest): Observable<Coupon> {
    return this.http.post<Coupon>(`${environment.apiUrl}/admin/coupons`, req);
  }

  deleteCoupon(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/coupons/${id}`);
  }
}
