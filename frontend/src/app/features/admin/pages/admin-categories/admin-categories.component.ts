import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Category } from '../../../../core/models/product.model';
import { NgIf, NgStyle, NgClass } from '@angular/common';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, NgStyle, NgClass],
  templateUrl: './admin-categories.component.html',
  styleUrl: './admin-categories.component.scss'
})
export class AdminCategoriesComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<Category[]>([]);
  readonly flattenedCategories = signal<(Category & { level: number; hasChildren: boolean; expanded: boolean })[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);
  
  private expandedCategories = new Set<number>();

  readonly form = this.fb.group({
    name: ['', Validators.required],
    slug: ['', Validators.required],
    description: [''],
    parentId: [null as number | null]
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.productService.getCategories().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.flattenCategories(cats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  flattenCategories(cats: Category[], level = 0, result: (Category & { level: number; hasChildren: boolean; expanded: boolean })[] = []) {
    for (const c of cats) {
      const hasChildren = c.children && c.children.length > 0;
      const expanded = this.expandedCategories.has(c.id);
      
      result.push({ ...c, level, hasChildren: !!hasChildren, expanded });
      
      if (hasChildren && expanded) {
        this.flattenCategories(c.children!, level + 1, result);
      }
    }
    this.flattenedCategories.set(result);
  }

  toggleCategory(id: number): void {
    if (this.expandedCategories.has(id)) {
      this.expandedCategories.delete(id);
    } else {
      this.expandedCategories.add(id);
    }
    this.flattenCategories(this.categories());
  }

  toggleShowForm(): void {
    this.showForm.set(!this.showForm());
    if (!this.showForm()) {
      this.resetForm();
    }
  }

  openAddSubcategory(parentId: number): void {
    this.resetForm();
    this.form.patchValue({ parentId });
    this.showForm.set(true);
  }

  editCategory(cat: Category): void {
    this.editingId.set(cat.id);
    this.form.patchValue({
      name: cat.name,
      slug: cat.slug,
      description: cat.description,
      parentId: cat.parentId || null
    });
    this.showForm.set(true);
  }

  deleteCategory(cat: Category): void {
    if (cat.children && cat.children.length > 0) {
      this.toast.error('Alt kategorisi olan bir kategori silinemez!');
      return;
    }
    if (confirm(`'${cat.name}' kategorisini silmek istediğinize emin misiniz?`)) {
      this.productService.deleteCategory(cat.id).subscribe({
        next: () => {
          this.toast.success('Kategori silindi.');
          this.loadCategories();
        },
        error: () => this.toast.error('Silme başarısız.')
      });
    }
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    
    const formVal = this.form.value;
    const payload: Partial<Category> & { parentId?: number } = {
      name: formVal.name || '',
      slug: formVal.slug || '',
      description: formVal.description || ''
    };
    if (formVal.parentId) {
      payload.parentId = formVal.parentId;
    }

    const req$ = this.editingId() 
      ? this.productService.updateCategory(this.editingId()!, payload)
      : this.productService.createCategory(payload);

    req$.subscribe({
        next: () => {
            this.toast.success(this.editingId() ? 'Kategori güncellendi' : 'Kategori eklendi');
            this.saving.set(false);
            this.resetForm();
            this.showForm.set(false);
            this.loadCategories();
        },
        error: () => {
            this.toast.error('İşlem başarısız');
            this.saving.set(false);
        }
    });
  }

  resetForm() {
    this.form.reset();
    this.editingId.set(null);
  }
}
