import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Product, ProductSummary, Category } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule],
  templateUrl: './admin-products.component.html',
  styleUrl: './admin-products.component.scss'
})
export class AdminProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly products = signal<ProductSummary[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editId = signal<number | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(1);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    sku: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    discountedPrice: [null as number | null],
    stockQuantity: [0],
    brand: [''],
    categoryId: [null as number | null],
    isActive: [true]
  });

  ngOnInit(): void {
    this.loadCategories();
    this.load();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: cats => this.categories.set(cats),
      error: () => {}
    });
  }

  load(): void {
    this.loading.set(true);
    this.productService.getAdminProducts(this.page(), 20).subscribe({
      next: res => { this.products.set(res.content); this.totalPages.set(res.totalPages); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openForm(): void { this.editId.set(null); this.form.reset({ isActive: true, stockQuantity: 0, price: 0 }); this.showForm.set(true); }
  closeForm(): void { this.showForm.set(false); this.editId.set(null); }

  editProduct(p: ProductSummary): void {
    this.editId.set(p.id);
    this.form.patchValue({
      name: p.name,
      sku: p.sku ?? '',
      description: p.description,
      price: p.price,
      discountedPrice: p.discountedPrice ?? null,
      stockQuantity: p.stockQuantity,
      brand: p.brand,
      categoryId: p.categoryId ?? null,
      isActive: p.isActive ?? true
    });
    this.showForm.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const id = this.editId();
    const obs = id
      ? this.productService.updateProduct(id, this.form.value as Partial<Product>)
      : this.productService.createProduct(this.form.value as Partial<Product>);
    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.toast.success(id ? 'Ürün güncellendi.' : 'Ürün eklendi.');
        this.load();
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Hata oluştu.'); }
    });
  }

  deleteProduct(id: number): void {
    if (!confirm('Bu ürünü silmek istediğinize emin misiniz?')) return;
    this.productService.deleteProduct(id).subscribe({
      next: () => { this.products.update(ps => ps.filter(p => p.id !== id)); this.toast.success('Ürün silindi.'); },
      error: () => this.toast.error('Silinemedi.')
    });
  }

  goPage(p: number): void { this.page.set(p); this.load(); }

  /** Kategori listesini düzleştirir (ağaç → düz liste) */
  flatCategories(): Category[] {
    const result: Category[] = [];
    const flatten = (cats: Category[], depth = 0): void => {
      cats.forEach(c => {
        result.push({ ...c, name: (depth > 0 ? '  '.repeat(depth) + '↳ ' : '') + c.name });
        if (c.children && c.children.length > 0) flatten(c.children, depth + 1);
      });
    };
    flatten(this.categories());
    return result;
  }
}
