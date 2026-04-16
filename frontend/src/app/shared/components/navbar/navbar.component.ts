import { Component, inject, signal, HostListener, effect, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProductService } from '../../../core/services/product.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly productService = inject(ProductService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly scrolled = signal(false);
  readonly mobileOpen = signal(false);
  readonly userMenuOpen = signal(false);

  // Persistent search query — stays in the input box after searching
  searchQuery = '';

  constructor() {
    effect(() => {
      if (this.auth.isLoggedIn() && !this.cartService.cart()) {
        this.cartService.getCart().subscribe();
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    // Keep search input populated when returning to the products page
    this.route.queryParamMap.subscribe(params => {
      const q = params.get('q');
      if (q !== null) {
        this.searchQuery = q;
      }
    });
  }

  toggleUserMenu(): void { this.userMenuOpen.update(v => !v); }
  toggleMobile(): void { this.mobileOpen.update(v => !v); }

  readonly initials = () => {
    const u = this.auth.currentUser();
    if (!u) return '';
    return `${u.firstName?.[0] ?? ''}${u.lastName?.[0] ?? ''}`.toUpperCase();
  };
  readonly roleLabel = () => { const r = this.auth.userRole(); return r === 'ADMIN' ? 'Admin' : r === 'SELLER' ? 'Satıcı' : 'Kullanıcı'; };
  readonly roleBadgeClass = () => { const r = this.auth.userRole(); return r === 'ADMIN' ? 'badge badge-danger' : r === 'SELLER' ? 'badge badge-warning' : 'badge badge-primary'; };

  @HostListener('window:scroll') onScroll(): void { this.scrolled.set(window.scrollY > 10); }
  @HostListener('document:click') onDocClick(): void { this.userMenuOpen.set(false); }
  @HostListener('document:keydown.escape') onDocEscape(): void {
    this.userMenuOpen.set(false);
  }

  doSearch(): void {
    this.productService.clearAiFilter();
    const q = this.searchQuery.trim();
    if (q) {
      this.router.navigate(['/products'], { queryParams: { q } });
    } else {
      // Clear search — go back to all products
      this.router.navigate(['/products'], { queryParams: {} });
    }
  }

  logout(): void {
    this.userMenuOpen.set(false);
    this.auth.logout().subscribe({ next: () => this.toast.success('Çıkış yapıldı.'), error: () => { } });
  }
}
