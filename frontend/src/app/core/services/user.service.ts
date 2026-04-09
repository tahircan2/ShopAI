import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, AddressRequest, AddressResponse, Notification } from '../models/user.model';
import { WishlistItem, ProductSummary } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users/me`;

  getProfile(): Observable<User> {
    return this.http.get<User>(this.api);
  }

  updateProfile(data: Partial<User>): Observable<User> {
    return this.http.put<User>(this.api, data);
  }

  // Addresses
  getAddresses(): Observable<AddressResponse[]> {
    return this.http.get<AddressResponse[]>(`${this.api}/addresses`);
  }

  createAddress(req: AddressRequest): Observable<AddressResponse> {
    return this.http.post<AddressResponse>(`${this.api}/addresses`, req);
  }

  updateAddress(id: number, req: AddressRequest): Observable<AddressResponse> {
    return this.http.put<AddressResponse>(`${this.api}/addresses/${id}`, req);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/addresses/${id}`);
  }

  // Notifications
  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.api}/notifications`);
  }

  markNotificationRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.api}/notifications/${id}/read`, {});
  }

  // Wishlist
  getWishlist(): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${this.api}/wishlist`);
  }

  addToWishlist(productId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/wishlist/${productId}`, {});
  }

  removeFromWishlist(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/wishlist/${productId}`);
  }

  // Reviews
  getMyReviews(page = 0, size = 10): Observable<{ content: any[]; totalElements: number }> {
    return this.http.get<{ content: any[]; totalElements: number }>(`${this.api}/reviews`, {
      params: { page, size }
    });
  }

  // Admin
  getAllUsers(page = 0, size = 20, search?: string, role?: string): Observable<{ content: User[]; totalElements: number }> {
    let params: any = { page, size };
    if (search) params.search = search;
    if (role) params.role = role;
    return this.http.get<{ content: User[]; totalElements: number }>(`${environment.apiUrl}/admin/users`, {
      params
    });
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/users/${userId}`);
  }

  updateUserRole(userId: number, role: string): Observable<User> {
    return this.http.put<User>(`${environment.apiUrl}/admin/users/${userId}/role`, { role });
  }

  toggleUserActive(userId: number): Observable<User> {
    return this.http.put<User>(`${environment.apiUrl}/admin/users/${userId}/toggle-active`, {});
  }
}
