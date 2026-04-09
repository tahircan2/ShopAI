import { Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { WishlistService } from '../../core/services/wishlist.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { WishlistItem } from '../../core/models/product.model';
import { CurrencyFormatPipe } from '../../shared/pipes/shared-pipes';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [RouterLink, CurrencyFormatPipe],
  templateUrl: './wishlist.component.html',
  styleUrl: './wishlist.component.scss'
})
export class WishlistComponent implements OnInit {
  private readonly wishlistService = inject(WishlistService);
  private readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly items = this.wishlistService.items;
  readonly loading = signal(true);
  readonly updatingCart = signal<number | null>(null);

  ngOnInit(): void {
    this.wishlistService.load().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
  }

  removeFromWishlist(productId: number): void {
    this.wishlistService.remove(productId).subscribe({
      next: () => {
        this.toast.success('Favorilerden çıkarıldı.');
      },
      error: () => this.toast.error('Hata oluştu.')
    });
  }

  addToCart(item: WishlistItem): void {
    this.updatingCart.set(item.id);
    this.cartService.addToCart({ productId: item.id, quantity: 1 }).subscribe({
      next: () => {
        this.updatingCart.set(null);
        this.toast.success('Ürün sepete eklendi.');
      },
      error: (err) => {
        this.updatingCart.set(null);
        // Varyant seçimi gerekiyorsa (400 Bad Request vb.) ürün detayına yönlendir
        if (err.status === 400 || err.error?.message?.toLowerCase().includes('variant')) {
          this.toast.info('Lütfen önce seçenekleri (beden/renk) belirleyin.');
          this.router.navigate(['/products', item.slug]);
        } else {
          this.toast.error('Sepete eklenirken işlem başarısız oldu.');
        }
      }
    });
  }
}
