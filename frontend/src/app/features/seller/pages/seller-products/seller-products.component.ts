import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Product, ProductSummary, Category, ProductImage } from '../../../../core/models/product.model';

interface ImagePreview {
  file: File;
  url: string;        // Object URL for local preview
}

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

  // ─── Image State ──────────────────────────────────────────────────
  readonly selectedImages = signal<ImagePreview[]>([]);
  readonly existingImages = signal<ProductImage[]>([]);
  readonly uploadingImages = signal(false);

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
    if (!this.showForm()) { this.resetForm(); }
  }

  showErr(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.resetForm();
  }

  private resetForm(): void {
    this.editId.set(null);
    this.form.reset({ isActive: true });
    this.clearSelectedImages();
    this.existingImages.set([]);
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
    this.clearSelectedImages();

    // Load existing images for this product
    this.productService.getProductById(p.id).subscribe({
      next: product => this.existingImages.set(product.images ?? []),
      error: () => {}
    });

    this.showForm.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ─── Image Handling ──────────────────────────────────────────────

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;

    const maxTotal = 8;
    const currentExisting = this.existingImages().length;
    const currentSelected = this.selectedImages().length;
    const remaining = maxTotal - currentExisting - currentSelected;

    const files = Array.from(input.files).slice(0, remaining);
    const previews: ImagePreview[] = files.map(file => ({
      file,
      url: URL.createObjectURL(file)
    }));

    this.selectedImages.update(prev => [...prev, ...previews]);

    // Reset file input so re-selecting the same file works
    input.value = '';
  }

  removeSelectedImage(index: number): void {
    this.selectedImages.update(prev => {
      const copy = [...prev];
      URL.revokeObjectURL(copy[index].url); // Free memory
      copy.splice(index, 1);
      return copy;
    });
  }

  removeExistingImage(productId: number, imageId: number): void {
    this.productService.deleteProductImage(productId, imageId).subscribe({
      next: () => {
        this.existingImages.update(imgs => imgs.filter(i => i.id !== imageId));
        this.toast.success('Resim silindi.');
      },
      error: () => this.toast.error('Resim silinemedi.')
    });
  }

  private clearSelectedImages(): void {
    this.selectedImages().forEach(p => URL.revokeObjectURL(p.url));
    this.selectedImages.set([]);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const files = event.dataTransfer?.files;
    if (!files) return;

    const maxTotal = 8;
    const currentExisting = this.existingImages().length;
    const currentSelected = this.selectedImages().length;
    const remaining = maxTotal - currentExisting - currentSelected;

    const validFiles = Array.from(files)
      .filter(f => ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(f.type))
      .slice(0, remaining);

    const previews: ImagePreview[] = validFiles.map(file => ({
      file,
      url: URL.createObjectURL(file)
    }));

    this.selectedImages.update(prev => [...prev, ...previews]);
  }

  // ─── Save ────────────────────────────────────────────────────────

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
      next: (saved) => {
        const productId = (saved as any).id ?? id;
        const filesToUpload = this.selectedImages().map(p => p.file);

        if (filesToUpload.length > 0 && productId) {
          this.uploadingImages.set(true);
          this.productService.uploadProductImages(productId, filesToUpload).subscribe({
            next: () => {
              this.uploadingImages.set(false);
              this.saving.set(false);
              this.closeForm();
              this.toast.success(id ? 'Ürün güncellendi ve resimler yüklendi.' : 'Ürün eklendi ve resimler yüklendi.');
              this.load();
            },
            error: (err) => {
              this.uploadingImages.set(false);
              this.saving.set(false);
              this.toast.error('Ürün kaydedildi fakat resimler yüklenirken hata oluştu: ' + (err.error?.message ?? ''));
              this.closeForm();
              this.load();
            }
          });
        } else {
          this.saving.set(false);
          this.closeForm();
          this.toast.success(id ? 'Ürün güncellendi.' : 'Ürün eklendi.');
          this.load();
        }
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
