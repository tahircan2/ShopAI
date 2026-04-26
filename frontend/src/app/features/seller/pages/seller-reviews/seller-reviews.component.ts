import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../../core/services/product.service';
import { ProductReviewList } from '../../../../core/models/product.model';
import { LucideAngularModule, Star, MessageSquare, ChevronDown, ChevronUp, ExternalLink } from 'lucide-angular';

@Component({
  selector: 'app-seller-reviews',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './seller-reviews.component.html',
  styleUrl: './seller-reviews.component.scss'
})
export class SellerReviewsComponent {
  private readonly productService = inject(ProductService);

  readonly productsWithReviews = signal<ProductReviewList[]>([]);
  readonly loading = signal(true);
  readonly expandedProductIds = signal<Set<number>>(new Set());

  readonly starIcon = Star;
  readonly messageIcon = MessageSquare;
  readonly chevronDown = ChevronDown;
  readonly chevronUp = ChevronUp;
  readonly externalLink = ExternalLink;

  constructor() {
    this.loadReviews();
  }

  loadReviews() {
    this.loading.set(true);
    this.productService.getMyProductReviews().subscribe({
      next: (res) => {
        this.productsWithReviews.set(res.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleExpand(productId: number) {
    const expanded = new Set(this.expandedProductIds());
    if (expanded.has(productId)) {
      expanded.delete(productId);
    } else {
      expanded.add(productId);
    }
    this.expandedProductIds.set(expanded);
  }

  isExpanded(productId: number): boolean {
    return this.expandedProductIds().has(productId);
  }
}
