import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyFormatPipe } from '../../shared/pipes/shared-pipes';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, FormsModule, CurrencyFormatPipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss'
})
export class CartComponent implements OnInit {
  readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);
  readonly loading = signal(true);
  couponCode = '';

  ngOnInit(): void {
    this.cartService.getCart().subscribe({ next: () => this.loading.set(false), error: () => this.loading.set(false) });
  }

  updateQty(itemId: number, qty: number): void {
    if (qty < 1) return;
    this.cartService.updateQuantity(itemId, qty).subscribe({
      error: () => this.toast.error('Miktar güncellenemedi.')
    });
  }

  removeItem(itemId: number): void {
    this.cartService.removeItem(itemId).subscribe({
      next: () => this.toast.success('Ürün sepetten çıkarıldı.'),
      error: () => this.toast.error('Ürün kaldırılamadı.')
    });
  }

  clearCart(): void {
    this.cartService.clearCart().subscribe({
      next: () => this.toast.info('Sepet temizlendi.'),
      error: () => this.toast.error('Hata oluştu.')
    });
  }

  applyCoupon(): void {
    if (!this.couponCode) return;
    this.cartService.applyCoupon({ code: this.couponCode }).subscribe({
      next: () => this.toast.success('Kupon uygulandı!'),
      error: (err) => this.toast.error(err.error?.message ?? 'Geçersiz kupon kodu.')
    });
  }

  removeCoupon(): void {
    this.cartService.removeCoupon().subscribe({
      next: () => { this.couponCode = ''; this.toast.info('Kupon kaldırıldı.'); },
      error: () => this.toast.error('Hata oluştu.')
    });
  }
}
