import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../core/services/toast.service';
import { ProductService } from '../../../../core/services/product.service';
import { Coupon } from '../../../../core/models/product.model';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './admin-coupons.component.html',
  styleUrl: './admin-coupons.component.scss'
})
export class AdminCouponsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly productService = inject(ProductService);

  readonly showForm = signal(false);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly coupons = signal<Coupon[]>([]);

  readonly form = this.fb.group({
    code: ['', Validators.required],
    discountType: ['PERCENTAGE', Validators.required],
    discountValue: [0, [Validators.required, Validators.min(0.01)]],
    minOrderAmount: [null as number | null],
    maxUses: [null as number | null],
    validFrom: ['', Validators.required],
    validUntil: ['', Validators.required]
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.productService.getCoupons().subscribe({
      next: cs => { this.coupons.set(cs); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  toggleShowForm(): void {
    this.showForm.update(v => !v);
    if (!this.showForm()) this.form.reset({ discountType: 'PERCENTAGE' });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.value;
    this.productService.createCoupon({
      code: v.code!,
      discountType: v.discountType as 'PERCENTAGE' | 'FIXED',
      discountValue: v.discountValue!,
      minOrderAmount: v.minOrderAmount ?? undefined,
      maxUses: v.maxUses ?? undefined,
      validFrom: v.validFrom!,
      validUntil: v.validUntil!
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Kupon oluşturuldu.');
        this.showForm.set(false);
        this.form.reset({ discountType: 'PERCENTAGE' });
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Kupon oluşturulamadı.');
      }
    });
  }

  delete(id: number): void {
    if (!confirm('Bu kuponu kalıcı olarak silmek istediğinizden emin misiniz?')) return;
    this.productService.deleteCoupon(id).subscribe({
      next: () => { this.coupons.update(cs => cs.filter(c => c.id !== id)); this.toast.success('Kupon silindi.'); },
      error: () => this.toast.error('Silinemedi.')
    });
  }

  discountLabel(c: Coupon): string {
    return c.discountType === 'PERCENTAGE' ? `%${c.discountValue}` : `${c.discountValue} ₺`;
  }
}
