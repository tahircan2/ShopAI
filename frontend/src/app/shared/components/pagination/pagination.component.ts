import { Component, Input, Output, EventEmitter, computed } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss'
})
export class PaginationComponent {
  @Input({ required: true }) totalPages = 1;
  @Input({ required: true }) currentPage = 0;
  
  @Output() pageChange = new EventEmitter<number>();

  readonly pageNumbers = computed(() => {
    let start = Math.max(0, this.currentPage - 2);
    let end = Math.min(this.totalPages - 1, start + 4);
    if (end - start < 4) {
      start = Math.max(0, end - 4);
    }
    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  });

  goPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
