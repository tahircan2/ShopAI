import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './admin-audit.component.html',
  styleUrl: './admin-audit.component.scss'
})
export class AdminAuditComponent implements OnInit {
  private readonly http = inject(HttpClient);

  readonly logs = signal<any[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/audit-logs`, { withCredentials: true }).subscribe({
      next: l => {
        this.logs.set(l);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  actionBadge(action: string): string {
    if (action.includes('DELETE') || action.includes('INJECTION')) return 'badge badge-danger';
    if (action.includes('LOGIN') || action.includes('LOGOUT')) return 'badge badge-info';
    if (action.includes('CREATE')) return 'badge badge-success';
    return 'badge badge-muted';
  }
}
