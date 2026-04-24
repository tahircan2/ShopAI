import { Component, inject, signal, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';
import { SellerStatsService } from '../../../../core/services/seller-stats.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.scss'
})
export class SellerDashboardComponent implements OnInit, AfterViewInit {
  readonly auth = inject(AuthService);
  private readonly stats = inject(SellerStatsService);

  @ViewChild('revenueCanvas') revenueCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryCanvas') categoryCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('topProductsCanvas') topProductsCanvas!: ElementRef<HTMLCanvasElement>;

  readonly loading = signal(true);
  readonly revenue = signal<any>(null);
  readonly orders = signal<any>(null);
  readonly customers = signal<any>(null);
  readonly avgRating = signal<any>(null);
  readonly recentOrders = signal<any[]>([]);
  readonly revenueChartData = signal<any[]>([]);
  readonly categoryData = signal<any[]>([]);
  readonly topProducts = signal<any[]>([]);

  private revenueChart: Chart | null = null;
  private categoryChart: Chart | null = null;
  private topProductsChart: Chart | null = null;

  ngOnInit(): void {
    this.loadAllData();
  }

  ngAfterViewInit(): void {
    // Charts will be rendered after data loads
  }

  private loadAllData(): void {
    let loaded = 0;
    const total = 7;
    const check = () => { loaded++; if (loaded >= total) this.loading.set(false); };

    this.stats.getRevenue().subscribe({ next: d => { this.revenue.set(d); check(); }, error: check });
    this.stats.getOrders().subscribe({ next: d => { this.orders.set(d); check(); }, error: check });
    this.stats.getCustomerCount().subscribe({ next: d => { this.customers.set(d); check(); }, error: check });
    this.stats.getAvgRating().subscribe({ next: d => { this.avgRating.set(d); check(); }, error: check });
    this.stats.getRevenueChart(7).subscribe({ next: d => { this.revenueChartData.set(d); this.renderRevenueChart(); check(); }, error: check });
    this.stats.getCategoryChart().subscribe({ next: d => { this.categoryData.set(d); this.renderCategoryChart(); check(); }, error: check });
    this.stats.getTopProducts().subscribe({ next: d => { this.topProducts.set(d); this.renderTopProductsChart(); check(); }, error: check });

    this.stats.getRecentOrders().subscribe({ next: d => this.recentOrders.set(d) });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'badge-warning', 'CONFIRMED': 'badge-info', 'SHIPPED': 'badge-purple',
      'DELIVERED': 'badge-success', 'CANCELLED': 'badge-danger', 'REFUNDED': 'badge-danger'
    };
    return map[status] || 'badge-default';
  }

  private renderRevenueChart(): void {
    setTimeout(() => {
      if (!this.revenueCanvas?.nativeElement) return;
      const data = this.revenueChartData();
      if (this.revenueChart) this.revenueChart.destroy();

      this.revenueChart = new Chart(this.revenueCanvas.nativeElement, {
        type: 'line',
        data: {
          labels: data.map(d => d.date.substring(5)),
          datasets: [{
            label: 'Gelir (₺)',
            data: data.map(d => d.revenue),
            borderColor: '#6366f1',
            backgroundColor: 'rgba(99,102,241,0.1)',
            fill: true,
            tension: 0.4,
            pointBackgroundColor: '#6366f1',
            pointBorderWidth: 2,
            pointRadius: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(99,102,241,0.08)' } },
            y: { ticks: { color: '#94a3b8', callback: (v) => '₺' + v }, grid: { color: 'rgba(99,102,241,0.08)' } }
          }
        }
      });
    }, 100);
  }

  private renderCategoryChart(): void {
    setTimeout(() => {
      if (!this.categoryCanvas?.nativeElement) return;
      const data = this.categoryData();
      if (this.categoryChart) this.categoryChart.destroy();

      const colors = ['#6366f1', '#22d3ee', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
      this.categoryChart = new Chart(this.categoryCanvas.nativeElement, {
        type: 'doughnut',
        data: {
          labels: data.map(d => d.category),
          datasets: [{
            data: data.map(d => d.revenue),
            backgroundColor: colors.slice(0, data.length),
            borderWidth: 0,
            hoverOffset: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: '65%',
          plugins: {
            legend: { position: 'right', labels: { color: '#e2e8f0', padding: 12, usePointStyle: true } }
          }
        }
      });
    }, 100);
  }

  private renderTopProductsChart(): void {
    setTimeout(() => {
      if (!this.topProductsCanvas?.nativeElement) return;
      const data = this.topProducts();
      if (this.topProductsChart) this.topProductsChart.destroy();

      this.topProductsChart = new Chart(this.topProductsCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: data.map(d => d.productName.length > 20 ? d.productName.substring(0, 20) + '…' : d.productName),
          datasets: [{
            label: 'Satış Adedi',
            data: data.map(d => d.totalQuantity),
            backgroundColor: 'rgba(99,102,241,0.7)',
            borderColor: '#6366f1',
            borderWidth: 1,
            borderRadius: 6
          }]
        },
        options: {
          indexAxis: 'y',
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(99,102,241,0.08)' } },
            y: { ticks: { color: '#e2e8f0' }, grid: { display: false } }
          }
        }
      });
    }, 100);
  }
}
