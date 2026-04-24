import { Component, Input, signal, computed } from '@angular/core';


/**
 * ChatDataTableComponent — AI chatbot yanıtlarında tablo tipinde
 * veri görselleştirmesi için dinamik, sayfalı, sıralanabilir tablo.
 *
 * Özellikler:
 * - Otomatik kolon çıkarımı (Object.keys)
 * - Sayfalama (10 satır/sayfa)
 * - Kolon sıralama (asc/desc)
 * - CSV indirme
 * - Dark theme tasarım
 */
@Component({
  selector: 'app-chat-data-table',
  standalone: true,
  imports: [],
  templateUrl: './chat-data-table.component.html',
  styleUrl: './chat-data-table.component.scss'
})
export class ChatDataTableComponent {
  @Input() set data(value: Record<string, unknown>[]) {
    this._data.set(value || []);
    this.currentPage.set(0);
    this.sortColumn.set(null);
    this.sortDir.set('asc');
  }

  @Input() maxRows = 50;
  @Input() pageSize = 10;

  readonly _data = signal<Record<string, unknown>[]>([]);
  readonly currentPage = signal(0);
  readonly sortColumn = signal<string | null>(null);
  readonly sortDir = signal<'asc' | 'desc'>('asc');
  readonly filterText = signal('');

  readonly columns = computed(() => {
    const d = this._data();
    if (!d.length) return [];
    return Object.keys(d[0]);
  });

  readonly filteredData = computed(() => {
    let rows = this._data().slice(0, this.maxRows);
    const filter = this.filterText().toLowerCase().trim();

    if (filter) {
      rows = rows.filter(row =>
        Object.values(row).some(v =>
          String(v ?? '').toLowerCase().includes(filter)
        )
      );
    }

    const col = this.sortColumn();
    const dir = this.sortDir();
    if (col) {
      rows = [...rows].sort((a, b) => {
        const va = a[col];
        const vb = b[col];
        if (va == null && vb == null) return 0;
        if (va == null) return 1;
        if (vb == null) return -1;
        if (typeof va === 'number' && typeof vb === 'number') {
          return dir === 'asc' ? va - vb : vb - va;
        }
        const sa = String(va);
        const sb = String(vb);
        return dir === 'asc' ? sa.localeCompare(sb) : sb.localeCompare(sa);
      });
    }

    return rows;
  });

  readonly totalPages = computed(() =>
    Math.ceil(this.filteredData().length / this.pageSize)
  );

  readonly pagedData = computed(() => {
    const start = this.currentPage() * this.pageSize;
    return this.filteredData().slice(start, start + this.pageSize);
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  toggleSort(col: string): void {
    if (this.sortColumn() === col) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(col);
      this.sortDir.set('asc');
    }
    this.currentPage.set(0);
  }

  getSortIcon(col: string): string {
    if (this.sortColumn() !== col) return '⇅';
    return this.sortDir() === 'asc' ? '↑' : '↓';
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  onFilter(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.filterText.set(input.value);
    this.currentPage.set(0);
  }

  formatCell(value: unknown): string {
    if (value == null) return '—';
    if (typeof value === 'number') {
      return value % 1 === 0 ? String(value) : value.toFixed(2);
    }
    const s = String(value);
    return s.length > 50 ? s.substring(0, 47) + '...' : s;
  }

  isNumeric(value: unknown): boolean {
    return typeof value === 'number';
  }

  fullText(value: unknown): string {
    return String(value ?? '');
  }

  downloadCsv(): void {
    const cols = this.columns();
    const rows = this.filteredData();
    if (!cols.length || !rows.length) return;

    const header = cols.join(',');
    const body = rows.map(row =>
      cols.map(c => {
        const v = row[c];
        const s = String(v ?? '');
        return s.includes(',') ? `"${s}"` : s;
      }).join(',')
    ).join('\n');

    const blob = new Blob([header + '\n' + body], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'analytics-data.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
