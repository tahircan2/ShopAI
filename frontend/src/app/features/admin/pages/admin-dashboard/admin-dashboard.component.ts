import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../../environments/environment';
import { AdminStats } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly stats = signal<AdminStats | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.http.get<AdminStats>(`${environment.apiUrl}/admin/stats`, { withCredentials: true }).subscribe({
      next: s => { this.stats.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
