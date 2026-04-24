import { Component, inject, signal, OnInit, ViewChild, ElementRef } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminStatsService } from '../../../../core/services/admin-stats.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  private readonly statsService = inject(AdminStatsService);

  @ViewChild('revenueCanvas') revenueCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('roleCanvas') roleCanvas!: ElementRef<HTMLCanvasElement>;

  readonly loading = signal(true);
  readonly revenue = signal<any>(null);
  readonly users = signal<any>(null);
  readonly stores = signal<any>(null);
  readonly aov = signal<any>(null);
  readonly storeComparison = signal<any[]>([]);
  readonly auditLogs = signal<any[]>([]);

  private revenueChart: Chart | null = null;
  private roleChart: Chart | null = null;

  ngOnInit(): void {
    let loaded = 0;
    const total = 6;
    const check = () => { loaded++; if (loaded >= total) { this.loading.set(false); this.renderCharts(); } };

    this.statsService.getPlatformRevenue().subscribe({ next: d => { this.revenue.set(d); check(); }, error: check });
    this.statsService.getUsersStats().subscribe({ next: d => { this.users.set(d); check(); }, error: check });
    this.statsService.getStoresStats().subscribe({ next: d => { this.stores.set(d); check(); }, error: check });
    this.statsService.getAov().subscribe({ next: d => { this.aov.set(d); check(); }, error: check });
    this.statsService.getStoreComparison().subscribe({ next: d => { this.storeComparison.set(d); check(); }, error: check });
    this.statsService.getAuditLogs(15).subscribe({ next: d => { this.auditLogs.set(d); check(); }, error: check });
  }

  private renderCharts(): void {
    // Revenue Chart
    this.statsService.getPlatformRevenueChart(30).subscribe(data => {
      setTimeout(() => {
        if (!this.revenueCanvas?.nativeElement) return;
        if (this.revenueChart) this.revenueChart.destroy();

        this.revenueChart = new Chart(this.revenueCanvas.nativeElement, {
          type: 'line',
          data: {
            labels: data.map(d => d.date.substring(5)),
            datasets: [{
              label: 'Platform Gelir (₺)',
              data: data.map(d => d.revenue),
              borderColor: '#6366f1',
              backgroundColor: 'rgba(99,102,241,0.1)',
              fill: true,
              tension: 0.4,
              pointBackgroundColor: '#6366f1',
              pointRadius: 3
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              x: { ticks: { color: '#94a3b8', maxTicksLimit: 10 }, grid: { color: 'rgba(99,102,241,0.08)' } },
              y: { ticks: { color: '#94a3b8', callback: (v) => '₺' + v }, grid: { color: 'rgba(99,102,241,0.08)' } }
            }
          }
        });
      }, 100);
    });

    // Role Distribution
    this.statsService.getUserRoleDistribution().subscribe(data => {
      setTimeout(() => {
        if (!this.roleCanvas?.nativeElement) return;
        if (this.roleChart) this.roleChart.destroy();

        const colors: Record<string, string> = { USER: '#6366f1', SELLER: '#22d3ee', ADMIN: '#f59e0b' };
        const labels: Record<string, string> = { USER: 'Kullanıcı', SELLER: 'Satıcı', ADMIN: 'Admin' };

        this.roleChart = new Chart(this.roleCanvas.nativeElement, {
          type: 'doughnut',
          data: {
            labels: data.map(d => labels[d.role] || d.role),
            datasets: [{
              data: data.map(d => d.count),
              backgroundColor: data.map(d => colors[d.role] || '#8b5cf6'),
              borderWidth: 0,
              hoverOffset: 8
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '60%',
            plugins: {
              legend: { position: 'right', labels: { color: '#e2e8f0', padding: 12, usePointStyle: true } }
            }
          }
        });
      }, 100);
    });
  }

  getLogAction(action: string): string {
    const map: Record<string, string> = {
      'CREATE_PRODUCT': '📦 Ürün Ekleme', 'UPDATE_PRODUCT': '✏️ Ürün Güncelleme',
      'DELETE_PRODUCT': '🗑️ Ürün Silme', 'CREATE_ORDER': '🛒 Sipariş',
      'UPDATE_ORDER_STATUS': '📋 Durum Güncelleme', 'LOGIN': '🔑 Giriş',
      'REGISTER': '📝 Kayıt', 'UPDATE_USER_ROLE': '👥 Rol Değişikliği'
    };
    return map[action] || action;
  }
}
