import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { ProductSummary } from '../../core/models/product.model';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyFormatPipe } from '../../shared/pipes/shared-pipes';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CurrencyFormatPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private readonly productService = inject(ProductService);
  readonly auth = inject(AuthService);

  readonly products = signal<ProductSummary[]>([]);
  readonly loading = signal(true);

  readonly features = [
    { icon: '🤖', title: 'AI Asistan', desc: 'Doğal dille ürün arayın, "kırmızı spor ayakkabı 500₺ altı" gibi sorgularla anında sonuç alın.' },
    { icon: '🛡️', title: 'Güvenli Alışveriş', desc: 'HttpOnly cookie tabanlı JWT sistemi ile verileriniz her zaman güvende. XSS ve CSRF\'ye karşı korumalı.' },
    { icon: '⚡', title: 'Akıllı Öneriler', desc: 'Geçmiş alışverişlerinize ve tercihlerinize göre kişiselleştirilmiş ürün önerileri.' },
    { icon: '📦', title: 'Hızlı Kargo', desc: '500₺ üzeri alışverişlerde ücretsiz kargo. Siparişlerinizi gerçek zamanlı takip edin.' },
    { icon: '🔄', title: 'Kolay İade', desc: '14 gün içinde hiçbir soru sormadan iade garantisi. Müşteri memnuniyeti önceliğimiz.' },
    { icon: '🏪', title: 'Satıcı Paneli', desc: 'Kendi mağazanızı kurun, ürünlerinizi yönetin ve büyüyen müşteri kitlenize ulaşın.' }
  ];

  ngOnInit(): void {
    this.productService.getFeaturedProducts().subscribe({
      next: (products) => { this.products.set(products.slice(0, 8)); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  discountPct(p: ProductSummary): number {
    if (!p.discountedPrice) return 0;
    return Math.round((1 - p.discountedPrice / p.price) * 100);
  }

  scrollToFeatures(): void {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  }
}
