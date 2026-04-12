import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe, JsonPipe, NgIf } from '@angular/common';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [DatePipe, JsonPipe, NgIf],
  templateUrl: './admin-audit.component.html',
  styleUrl: './admin-audit.component.scss'
})
export class AdminAuditComponent implements OnInit {
  private readonly http = inject(HttpClient);

  readonly logs = signal<any[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(1);
  readonly loading = signal(true);
  
  readonly selectedLog = signal<any>(null);

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading.set(true);
    this.http.get<any>(`${environment.apiUrl}/admin/audit-logs?page=${this.page()}&size=20`, { withCredentials: true }).subscribe({
      next: res => {
        this.logs.set(res.content);
        this.totalPages.set(res.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  nextPage() {
    if (this.page() < this.totalPages() - 1) {
      this.page.update(p => p + 1);
      this.loadLogs();
    }
  }

  prevPage() {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadLogs();
    }
  }

  actionBadge(action: string): string {
    const a = action || '';
    if (a.includes('INJECTION')) return 'badge badge-danger';
    if (a.startsWith('AI_')) return 'badge badge-warning'; // AI actions (cart etc)
    if (a.includes('DELETE')) return 'badge badge-danger';
    if (a.includes('CREATE')) return 'badge badge-success';
    if (a.includes('UPDATE') || a.includes('CHANGE') || a.includes('ACTIVATE') || a.includes('TOGGLE')) return 'badge badge-primary';
    if (a.includes('LOGIN') || a.includes('LOGOUT')) return 'badge badge-info';
    return 'badge badge-muted';
  }

  getParsedData(data: any): any {
    if (!data) return null;
    if (typeof data === 'object') return data;
    try {
      const parsed = JSON.parse(data);
      // Recursively parse if it's still a string (escaped JSON)
      if (typeof parsed === 'string') return this.getParsedData(parsed);
      return parsed;
    } catch {
      return data;
    }
  }

  getKeys(data: any): string[] {
    const parsed = this.getParsedData(data);
    return parsed ? Object.keys(parsed) : [];
  }
}
