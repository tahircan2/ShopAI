import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss'
})
export class AdminShellComponent {
  readonly auth = inject(AuthService);
  readonly collapsed = signal(false);
  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  readonly navItems = [
    { path: '/admin/dashboard', icon: '📊', label: 'Dashboard' },
    { path: '/admin/users', icon: '👥', label: 'Kullanıcılar' },
    { path: '/admin/products', icon: '📦', label: 'Ürünler' },
    { path: '/admin/orders', icon: '🛒', label: 'Siparişler' },
    { path: '/admin/categories', icon: '📂', label: 'Kategoriler' },
    { path: '/admin/coupons', icon: '🎫', label: 'Kuponlar' },
    { path: '/admin/audit-logs', icon: '📋', label: 'Audit Loglar' },
  ];
}
