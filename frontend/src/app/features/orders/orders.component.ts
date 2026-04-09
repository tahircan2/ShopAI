import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { OrderSummary, WishlistItem } from '../../core/models/product.model';
import { CurrencyFormatPipe } from '../../shared/pipes/shared-pipes';

// ─── Orders Component ────────────────────────────────────────────────────────
@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterLink, DatePipe, CurrencyFormatPipe],
  templateUrl: './orders.component.html',
  styleUrl: './orders.component.scss'
})
export class OrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(ToastService);

  readonly orders = signal<OrderSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.orderService.getMyOrders().subscribe({
      next: (res) => {
        this.orders.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  cancelOrder(num: string): void {
    this.orderService.cancelOrder(num).subscribe({
      next: (updated) => {
        this.orders.update(os => os.map(o => o.orderNumber === num ? { ...o, status: updated.status } : o));
        this.toast.success('Sipariş iptal edildi.');
      },
      error: (err) => this.toast.error(err.error?.message ?? 'İptal edilemedi.')
    });
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'Bekliyor',
      CONFIRMED: 'Onaylandı',
      SHIPPED: 'Kargoda',
      DELIVERED: 'Teslim Edildi',
      CANCELLED: 'İptal',
      REFUNDED: 'İade'
    };
    return m[s] ?? s;
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'badge badge-warning',
      CONFIRMED: 'badge badge-info',
      SHIPPED: 'badge badge-primary',
      DELIVERED: 'badge badge-success',
      CANCELLED: 'badge badge-danger',
      REFUNDED: 'badge badge-muted'
    };
    return m[s] ?? 'badge badge-muted';
  }
}
