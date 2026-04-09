import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Category } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-categories.component.html',
  styleUrl: './admin-categories.component.scss'
})
export class AdminCategoriesComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    slug: ['', Validators.required],
    description: ['']
  });

  ngOnInit(): void {
    this.productService.getCategories().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleShowForm(): void {
    this.showForm.update(v => !v);
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    // POST /admin/categories - similar pattern
    this.toast.info('Kategori ekleme: /api/admin/categories endpoint bağlanacak.');
    this.saving.set(false);
    this.showForm.set(false);
  }
}
