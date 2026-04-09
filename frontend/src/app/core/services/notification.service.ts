import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/users/me/notifications`;

  readonly notifications = signal<Notification[]>([]);
  readonly unreadCount = signal(0);

  load() {
    return this.http.get<Notification[]>(this.api).pipe(
      tap(ns => {
        this.notifications.set(ns);
        this.unreadCount.set(ns.filter(n => !n.isRead).length);
      })
    );
  }

  markRead(id: number) {
    return this.http.put<void>(`${this.api}/${id}/read`, {}).pipe(
      tap(() => {
        this.notifications.update(ns =>
          ns.map(n => n.id === id ? { ...n, isRead: true } : n)
        );
        this.unreadCount.update(c => Math.max(0, c - 1));
      })
    );
  }

  markAllRead() {
    this.notifications.update(ns => ns.map(n => ({ ...n, isRead: true })));
    this.unreadCount.set(0);
  }
}
