import { Routes } from '@angular/router';

export const sellerRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./seller-shell.component').then(m => m.SellerShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/seller-dashboard/seller-dashboard.component').then(m => m.SellerDashboardComponent) },
      { path: 'products', loadComponent: () => import('./pages/seller-products/seller-products.component').then(m => m.SellerProductsComponent) },
      { path: 'orders', loadComponent: () => import('./pages/seller-orders/seller-orders.component').then(m => m.SellerOrdersComponent) },
      { path: 'shop', loadComponent: () => import('./pages/seller-shop/seller-shop.component').then(m => m.SellerShopComponent) },
    ]
  }
];
