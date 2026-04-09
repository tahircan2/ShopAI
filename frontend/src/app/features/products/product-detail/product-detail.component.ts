import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Product, ProductVariant, Review } from '../../../core/models/product.model';
import { CurrencyFormatPipe } from '../../../shared/pipes/shared-pipes';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, DatePipe, CurrencyFormatPipe],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss'
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly wishlistService = inject(WishlistService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly product = signal<Product | null>(null);
  readonly reviews = signal<Review[]>([]);
  readonly loading = signal(true);
  readonly loadingReviews = signal(false);
  readonly addingToCart = signal(false);
  readonly submittingReview = signal(false);
  readonly inWishlist = computed(() => {
    const pid = this.product()?.id;
    return pid ? this.wishlistService.isInWishlist(pid) : false;
  });
  readonly selectedImageIndex = signal(0);
  readonly selectedColor = signal<string | null>(null);
  readonly selectedSize = signal<string | null>(null);
  readonly qty = signal(1);
  readonly detailTab = signal<'desc' | 'reviews'>('desc');

  readonly hasUserReviewed = computed(() => {
    const usr = this.auth.currentUser();
    if (!usr) return false;
    return this.reviews().some(r => r.userId === usr.id);
  });

  readonly Math = Math;

  decQty(): void { this.qty.update(v => Math.max(1, v - 1)); }
  incQty(): void { this.qty.update(v => Math.min(this.maxStock(), v + 1)); }

  readonly reviewForm = this.fb.group({
    rating: [0, [Validators.required, Validators.min(1)]],
    title: [''],
    comment: ['']
  });

  readonly currentImage = computed(() => {
    const imgs = this.product()?.images ?? [];
    return imgs[this.selectedImageIndex()]?.imageUrl ?? null;
  });

  readonly colorOptions = computed(() => {
    const variants = this.product()?.variants ?? [];
    const seen = new Set<string>();
    return variants
      .filter(v => v.color && !seen.has(v.color) && seen.add(v.color))
      .map(v => ({ color: v.color, hex: v.colorHex }));
  });

  readonly sizeOptions = computed(() => {
    const variants = this.product()?.variants ?? [];
    const filtered = this.selectedColor()
      ? variants.filter(v => v.color === this.selectedColor())
      : variants;
    return [...new Set(filtered.map(v => v.size).filter(Boolean))] as string[];
  });

  readonly selectedVariant = computed((): ProductVariant | null => {
    const variants = this.product()?.variants ?? [];
    if (!variants.length) return null;
    return variants.find(v =>
      (!this.selectedColor() || v.color === this.selectedColor()) &&
      (!this.selectedSize() || v.size === this.selectedSize())
    ) ?? null;
  });

  readonly selectedVariantModifier = computed(() =>
    this.selectedVariant()?.priceModifier ?? 0
  );

  readonly finalPrice = computed(() => {
    const p = this.product();
    if (!p) return 0;
    const base = p.discountedPrice ?? p.price;
    return base + this.selectedVariantModifier();
  });

  readonly maxStock = computed(() =>
    this.selectedVariant()?.stockQuantity ?? this.product()?.stockQuantity ?? 0
  );

  readonly stockInfo = computed((): 'ok' | 'low' | 'out' => {
    const s = this.maxStock();
    if (s === 0) return 'out';
    if (s <= 5) return 'low';
    return 'ok';
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.productService.getProductBySlug(slug).subscribe({
      next: (p) => {
        this.product.set(p);
        this.loading.set(false);
        this.loadReviews(p.id);
      },
      error: () => this.loading.set(false)
    });
  }

  loadReviews(productId: number): void {
    this.loadingReviews.set(true);
    this.productService.getReviews(productId).subscribe({
      next: (res) => { this.reviews.set(res.content); this.loadingReviews.set(false); },
      error: () => this.loadingReviews.set(false)
    });
  }

  selectColor(color: string | undefined): void {
    if (!color) return;
    this.selectedColor.set(color);
    this.selectedSize.set(null); // reset size when color changes
  }

  selectSize(size: string): void {
    this.selectedSize.set(size);
  }

  discountPct(): number {
    const p = this.product();
    if (!p?.discountedPrice) return 0;
    return Math.round((1 - p.discountedPrice / p.price) * 100);
  }

  scrollToReviews(event: Event): void {
    event.preventDefault();
    this.detailTab.set('reviews');
    // small timeout to allow tab to render before scrolling
    setTimeout(() => {
      const el = document.getElementById('reviews');
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 50);
  }

  addToCart(): void {
    if (!this.auth.isLoggedIn()) {
      this.toast.info('Sepete eklemek için giriş yapınız.');
      this.router.navigate(['/auth/login']);
      return;
    }
    const p = this.product();
    if (!p) return;
    this.addingToCart.set(true);
    this.cartService.addToCart({
      productId: p.id,
      variantId: this.selectedVariant()?.id,
      quantity: this.qty()
    }).subscribe({
      next: () => { this.addingToCart.set(false); this.toast.success('Ürün sepete eklendi!'); },
      error: (err) => {
        this.addingToCart.set(false);
        this.toast.error(err.error?.message ?? 'Sepete eklenemedi.');
      }
    });
  }

  toggleWishlist(): void {
    if (!this.auth.isLoggedIn()) { this.toast.info('Favorilere eklemek için giriş yapın.'); return; }
    const pid = this.product()!.id;
    if (this.inWishlist()) {
      this.wishlistService.remove(pid).subscribe({
        next: () => { this.toast.info('Favorilerden çıkarıldı.'); }
      });
    } else {
      this.wishlistService.add(pid).subscribe({
        next: () => { this.toast.success('Favorilere eklendi!'); }
      });
    }
  }

  submitReview(): void {
    if (this.reviewForm.invalid || !this.reviewForm.value.rating) {
      this.toast.warning('Lütfen puan seçin.');
      return;
    }
    this.submittingReview.set(true);
    const pid = this.product()!.id;
    this.productService.submitReview(pid, {
      rating: this.reviewForm.value.rating!,
      title: this.reviewForm.value.title || undefined,
      comment: this.reviewForm.value.comment || undefined
    }).subscribe({
      next: (r) => {
        this.reviews.update(rs => [r, ...rs]);
        this.submittingReview.set(false);
        this.reviewForm.reset({ rating: 0 });
        this.toast.success('Değerlendirmeniz gönderildi!');
        // Refresh product to get updated ratingAvg/ratingCount
        this.productService.getProductBySlug(this.product()!.slug).subscribe(p => this.product.set(p));
      },
      error: (err) => {
        this.submittingReview.set(false);
        this.toast.error(err.error?.message ?? 'Değerlendirme gönderilemedi.');
      }
    });
  }

  deleteReview(reviewId: number): void {
    if (!confirm('Yorumu silmek istediğinize emin misiniz?')) return;
    const pid = this.product()!.id;
    this.productService.deleteReview(pid, reviewId).subscribe({
      next: () => {
        this.reviews.update(rs => rs.filter(r => r.id !== reviewId));
        this.toast.success('Yorum silindi.');
        // Refresh product rating
        this.productService.getProductBySlug(this.product()!.slug).subscribe(p => this.product.set(p));
      },
      error: (err) => this.toast.error(err.error?.message ?? 'Silinemedi.')
    });
  }
}
