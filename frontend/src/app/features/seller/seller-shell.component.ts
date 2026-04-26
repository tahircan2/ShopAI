import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-seller-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './seller-shell.component.html',
  styleUrl: './seller-shell.component.scss'
})
export class SellerShellComponent {
  readonly auth = inject(AuthService);
  readonly collapsed = signal(false);
  toggleCollapsed(): void { this.collapsed.update(v => !v); }
  readonly navItems = [
    { path: '/seller/dashboard', icon: '📊', label: 'Dashboard' },
    { path: '/seller/products', icon: '📦', label: 'Ürünlerim' },
    { path: '/seller/orders', icon: '🛒', label: 'Siparişler' },
    { path: '/seller/reviews', icon: '💬', label: 'Yorumlar' },
    { path: '/seller/shop', icon: '🏪', label: 'Mağaza Ayarları' },
  ];
  shopInitial(): string { return (this.auth.currentUser()?.shopName ?? 'M')[0].toUpperCase(); }
}
