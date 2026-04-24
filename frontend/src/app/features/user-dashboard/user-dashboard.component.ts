import { Component, inject, signal, OnInit, ViewChild, ElementRef } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UserStatsService } from '../../core/services/user-stats.service';
import { AuthService } from '../../core/services/auth.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [DecimalPipe, DatePipe, RouterLink],
  templateUrl: './user-dashboard.component.html',
  styleUrl: './user-dashboard.component.scss'
})
export class UserDashboardComponent implements OnInit {
  private readonly statsService = inject(UserStatsService);
  readonly auth = inject(AuthService);

  @ViewChild('spendCanvas') spendCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryCanvas') categoryCanvas!: ElementRef<HTMLCanvasElement>;

  readonly loading = signal(true);
  readonly personal = signal<any>(null);
  readonly recentOrders = signal<any[]>([]);

  private spendChart: Chart | null = null;
  private categoryChart: Chart | null = null;

  ngOnInit(): void {
    let loaded = 0;
    const total = 2;
    const check = () => { loaded++; if (loaded >= total) { this.loading.set(false); this.renderCharts(); } };

    this.statsService.getPersonalStats().subscribe({ next: d => { this.personal.set(d); check(); }, error: check });
    this.statsService.getRecentOrders(5).subscribe({ next: d => { this.recentOrders.set(d); check(); }, error: check });
  }

  getMembershipIcon(): string {
    const m = this.personal()?.membership;
    return m === 'Gold' ? '🥇' : m === 'Silver' ? '🥈' : '🥉';
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'badge-warning', 'CONFIRMED': 'badge-info', 'SHIPPED': 'badge-purple',
      'DELIVERED': 'badge-success', 'CANCELLED': 'badge-danger', 'REFUNDED': 'badge-danger'
    };
    return map[status] || 'badge-default';
  }

  private renderCharts(): void {
    // Monthly Spend
    this.statsService.getMonthlySpend().subscribe(data => {
      setTimeout(() => {
        if (!this.spendCanvas?.nativeElement) return;
        if (this.spendChart) this.spendChart.destroy();

        const months = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];
        this.spendChart = new Chart(this.spendCanvas.nativeElement, {
          type: 'bar',
          data: {
            labels: data.map(d => months[d.month - 1] + ' ' + d.year),
            datasets: [{
              label: 'Harcama (₺)',
              data: data.map(d => d.spend),
              backgroundColor: 'rgba(99,102,241,0.6)',
              borderColor: '#6366f1',
              borderWidth: 1,
              borderRadius: 8,
              maxBarThickness: 40
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              x: { ticks: { color: '#94a3b8' }, grid: { display: false } },
              y: { ticks: { color: '#94a3b8', callback: (v) => '₺' + v }, grid: { color: 'rgba(99,102,241,0.08)' } }
            }
          }
        });
      }, 100);
    });

    // Category Spend
    this.statsService.getCategorySpend().subscribe(data => {
      setTimeout(() => {
        if (!this.categoryCanvas?.nativeElement) return;
        if (this.categoryChart) this.categoryChart.destroy();

        const colors = ['#6366f1', '#22d3ee', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
        this.categoryChart = new Chart(this.categoryCanvas.nativeElement, {
          type: 'doughnut',
          data: {
            labels: data.map(d => d.category),
            datasets: [{
              data: data.map(d => d.spend),
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
    });
  }
}
