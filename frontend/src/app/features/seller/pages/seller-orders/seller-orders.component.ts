import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { OrderService } from '../../../../core/services/order.service';
import { OrderSummary } from '../../../../core/models/product.model';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './seller-orders.component.html',
  styleUrl: './seller-orders.component.scss'
})
export class SellerOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<OrderSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.orderService.getMyOrders().subscribe({
      next: r => {
        this.orders.set(r.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
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
