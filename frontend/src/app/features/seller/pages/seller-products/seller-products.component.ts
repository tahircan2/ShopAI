import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Product, ProductSummary, Category } from '../../../../core/models/product.model';

@Component({
  selector: 'app-seller-products',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './seller-products.component.html',
  styleUrl: './seller-products.component.scss'
})
export class SellerProductsComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly products = signal<ProductSummary[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editId = signal<number | null>(null);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    sku: ['', Validators.required],
    description: [''],
    longDescription: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    discountedPrice: [null as number | null],
    stockQuantity: [0],
    brand: [''],
    categoryId: [null as number | null],
    tags: [''],
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
    this.productService.getMyProducts(0, 50).subscribe({
      next: res => {
        this.products.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleShowForm(): void {
    this.showForm.update(v => !v);
    if (!this.showForm()) { this.editId.set(null); this.form.reset({ isActive: true }); }
  }

  showErr(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editId.set(null);
    this.form.reset({ isActive: true });
  }

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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const id = this.editId();

    const payload = { ...this.form.value } as any;
    if (typeof payload.tags === 'string') {
      payload.tags = payload.tags.split(',')
        .map((t: string) => t.trim())
        .filter((t: string) => t.length > 0);
    }

    const obs = id
      ? this.productService.updateProduct(id, payload)
      : this.productService.createProduct(payload);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.toast.success(id ? 'Ürün güncellendi.' : 'Ürün eklendi.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Bir hata oluştu.');
      }
    });
  }

  deleteProduct(id: number): void {
    if (!confirm('Silmek istediğinize emin misiniz?')) return;
    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.products.update(ps => ps.filter(p => p.id !== id));
        this.toast.success('Ürün silindi.');
      },
      error: () => this.toast.error('Silinemedi.')
    });
  }

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
