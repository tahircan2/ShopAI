import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { environment } from '../../../../../environments/environment';
import { SellerStats } from '../../../../core/models/product.model';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.scss'
})
export class SellerDashboardComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  readonly stats = signal<SellerStats | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.http.get<SellerStats>(`${environment.apiUrl}/seller/stats`, { withCredentials: true }).subscribe({
      next: s => {
        this.stats.set(s);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
