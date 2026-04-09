import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../core/services/order.service';
import { ToastService } from '../../../../core/services/toast.service';
import { OrderSummary } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.scss'
})
export class AdminOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(ToastService);

  readonly orders = signal<OrderSummary[]>([]);
  readonly loading = signal(true);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly page = signal(0);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.orderService.getAllOrders(this.page()).subscribe({
      next: res => {
        this.orders.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(Math.ceil(res.totalElements / 20));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goPage(p: number): void { this.page.set(p); this.load(); }

  updateStatus(order: OrderSummary, event: Event): void {
    const status = (event.target as HTMLSelectElement).value;
    this.orderService.updateOrderStatus(order.id, status).subscribe({
      next: updated => {
        // the backend returns a full OrderResponse, but we just need to update the status in the summary list.
        this.orders.update(os => os.map(o => o.id === updated.id ? { ...o, status: updated.status } : o));
        this.toast.success('Durum güncellendi.');
      },
      error: () => this.toast.error('Güncellenemedi.')
    });
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'Bekliyor',
      CONFIRMED: 'Onaylandı',
      SHIPPED: 'Kargoda',
      DELIVERED: 'Teslim',
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
