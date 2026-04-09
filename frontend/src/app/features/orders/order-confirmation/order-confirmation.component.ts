import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/product.model';
import { CurrencyFormatPipe } from '../../../shared/pipes/shared-pipes';

@Component({
  selector: 'app-order-confirmation',
  standalone: true,
  imports: [RouterLink, DatePipe, CurrencyFormatPipe],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.scss'
})
export class OrderConfirmationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);
  readonly copied = signal(false);
  readonly orderNumber = signal('');

  readonly statusSteps = [
    { key: 'PENDING',   label: 'Sipariş Alındı',    icon: '📋' },
    { key: 'CONFIRMED', label: 'Onaylandı',          icon: '✅' },
    { key: 'SHIPPED',   label: 'Kargoya Verildi',    icon: '📦' },
    { key: 'DELIVERED', label: 'Teslim Edildi',      icon: '🏠' }
  ];

  readonly statusOrder = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];

  readonly currentStatusIndex = computed(() => {
    const s = this.order()?.status ?? 'PENDING';
    const idx = this.statusOrder.indexOf(s);
    return idx === -1 ? 0 : idx;
  });

  readonly estimatedDelivery = computed(() => {
    const o = this.order();
    if (!o) return null;
    const d = new Date(o.createdAt);
    d.setDate(d.getDate() + 3);
    return d;
  });

  ngOnInit(): void {
    const num = this.route.snapshot.paramMap.get('orderNumber') ?? '';
    this.orderNumber.set(num);
    this.orderService.getOrderByNumber(num).subscribe({
      next: (o) => { this.order.set(o); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  copyOrderNumber(): void {
    navigator.clipboard.writeText(this.orderNumber()).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'Bekliyor', CONFIRMED: 'Onaylandı',
      SHIPPED: 'Kargoda', DELIVERED: 'Teslim Edildi',
      CANCELLED: 'İptal', REFUNDED: 'İade'
    };
    return m[s] ?? s;
  }
}
