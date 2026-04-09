import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProductSummary, ProductFilter, Category } from '../../../core/models/product.model';
import { CurrencyFormatPipe } from '../../../shared/pipes/shared-pipes';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, CurrencyFormatPipe, PaginationComponent],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss'
})
export class ProductListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly products = signal<ProductSummary[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly filterOpen = signal(false);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly currentPage = signal(0);
  readonly expandedCategories = signal<Set<number>>(new Set());

  readonly Math = Math;

  sortBy = 'createdAt,desc';
  filter: ProductFilter = { page: 0, size: 9 };

  readonly displayProducts = computed(() =>
    this.productService.aiFilteredProducts() ?? this.products()
  );

  ngOnInit(): void {
    this.productService.getCategories().subscribe(c => {
      this.categories.set(c);
      this.autoExpandActiveCategory();
    });

    // Reactively listen to query params so search from navbar updates the list
    this.route.queryParamMap.subscribe(params => {
      const q = params.get('q');
      const categorySlug = params.get('categorySlug');
      this.filter = { page: 0, size: 9 };
      if (q) { this.filter.q = q; }
      if (categorySlug) { 
        this.filter.categorySlug = categorySlug;
        this.autoExpandActiveCategory();
      }
      this.currentPage.set(0);
      this.loadProducts();
    });
  }

  private autoExpandActiveCategory(): void {
    const slug = this.filter.categorySlug;
    if (!slug) return;

    const findAndExpand = (cats: Category[]): boolean => {
      for (const cat of cats) {
        if (cat.slug === slug) return true;
        if (cat.children && cat.children.length > 0) {
          if (findAndExpand(cat.children)) {
            this.toggleCategory(cat.id); // Expand parent if child is active
            return true;
          }
        }
      }
      return false;
    };

    findAndExpand(this.categories());
  }

  toggleCategory(id: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }
    const set = new Set(this.expandedCategories());
    if (set.has(id)) set.delete(id);
    else set.add(id);
    this.expandedCategories.set(set);
  }

  isExpanded(id: number): boolean {
    return this.expandedCategories().has(id);
  }

  onCategorySelect(slug: string | undefined): void {
    this.filter.categorySlug = slug;
    this.applyFilters();
  }

  loadProducts(): void {
    this.loading.set(true);
    const [sortByField, sortDir] = this.sortBy.split(',');
    const f: ProductFilter = {
      ...this.filter,
      sortBy: sortByField as ProductFilter['sortBy'],
      sortDir: sortDir as 'asc' | 'desc',
    };

    this.productService.getProducts(f).subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  applyFilters(): void { this.filter.page = 0; this.currentPage.set(0); this.loadProducts(); }
  onSortChange(): void { this.applyFilters(); }

  clearFilters(): void {
    this.filter = { page: 0, size: 9 };
    this.sortBy = 'createdAt,desc';
    this.productService.clearAiFilter();
    this.router.navigate([], { queryParams: {}, replaceUrl: true });
    this.loadProducts();
  }

  clearAiFilter(): void { this.productService.clearAiFilter(); }

  goPage(page: number): void {
    this.filter.page = page;
    this.currentPage.set(page);
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  discountPct(p: ProductSummary): number {
    if (!p.discountedPrice) return 0;
    return Math.round((1 - p.discountedPrice / p.price) * 100);
  }

  addToCart(p: ProductSummary, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    if (!this.auth.isLoggedIn()) {
      this.toast.info('Sepete eklemek için giriş yapınız.');
      this.router.navigate(['/auth/login']);
      return;
    }

    if (p.hasVariants) {
      this.toast.info('Lütfen ürün seçeneklerini seçiniz.');
      this.router.navigate(['/products', p.slug], { queryParams: { focus: 'size' } });
      return;
    }

    this.cartService.addToCart({
      productId: p.id,
      quantity: 1
    }).subscribe({
      next: () => this.toast.success('Ürün sepete eklendi!'),
      error: (err) => this.toast.error(err.error?.message ?? 'Sepete eklenemedi.')
    });
  }
}
