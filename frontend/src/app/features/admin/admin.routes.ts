import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-shell.component').then(m => m.AdminShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'users', loadComponent: () => import('./pages/admin-users/admin-users.component').then(m => m.AdminUsersComponent) },
      { path: 'products', loadComponent: () => import('./pages/admin-products/admin-products.component').then(m => m.AdminProductsComponent) },
      { path: 'orders', loadComponent: () => import('./pages/admin-orders/admin-orders.component').then(m => m.AdminOrdersComponent) },
      { path: 'categories', loadComponent: () => import('./pages/admin-categories/admin-categories.component').then(m => m.AdminCategoriesComponent) },
      { path: 'coupons', loadComponent: () => import('./pages/admin-coupons/admin-coupons.component').then(m => m.AdminCouponsComponent) },
      { path: 'audit-logs', loadComponent: () => import('./pages/admin-audit/admin-audit.component').then(m => m.AdminAuditComponent) },
    ]
  }
];
