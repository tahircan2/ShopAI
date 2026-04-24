import { Routes } from '@angular/router';
import { authGuard, adminGuard, sellerGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ── Public ────────────────────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'products',
    loadComponent: () => import('./features/products/product-list/product-list.component').then(m => m.ProductListComponent)
  },
  {
    path: 'products/:slug',
    loadComponent: () => import('./features/products/product-detail/product-detail.component').then(m => m.ProductDetailComponent)
  },
  {
    path: 'about',
    loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent)
  },
  {
    path: 'contact',
    loadComponent: () => import('./features/contact/contact.component').then(m => m.ContactComponent)
  },

  // ── Legal Pages ───────────────────────────────────────────────────────────
  {
    path: 'legal',
    children: [
      {
        path: 'faq',
        loadComponent: () => import('./features/legal/faq/faq.component').then(m => m.FaqComponent)
      },
      {
        path: 'shipping',
        loadComponent: () => import('./features/legal/shipping/shipping.component').then(m => m.ShippingComponent)
      },
      {
        path: 'returns',
        loadComponent: () => import('./features/legal/returns/returns.component').then(m => m.ReturnsComponent)
      },
      {
        path: 'privacy',
        loadComponent: () => import('./features/legal/privacy/privacy.component').then(m => m.PrivacyComponent)
      },
      {
        path: 'terms',
        loadComponent: () => import('./features/legal/terms/terms.component').then(m => m.TermsComponent)
      },
      { path: '', redirectTo: 'faq', pathMatch: 'full' }
    ]
  },

  // ── Auth ──────────────────────────────────────────────────────────────────
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
      },
      {
        path: 'reset-password',
        loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
      },
      {
        path: 'verify-email',
        loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ── User Protected ────────────────────────────────────────────────────────
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cart/cart.component').then(m => m.CartComponent)
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./features/checkout/checkout.component').then(m => m.CheckoutComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders.component').then(m => m.OrdersComponent)
  },
  {
    path: 'order-confirmation/:orderNumber',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-confirmation/order-confirmation.component').then(m => m.OrderConfirmationComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'wishlist',
    canActivate: [authGuard],
    loadComponent: () => import('./features/wishlist/wishlist.component').then(m => m.WishlistComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/user-dashboard/user-dashboard.component').then(m => m.UserDashboardComponent)
  },
  {
    path: 'ai-chat',
    canActivate: [authGuard],
    loadComponent: () => import('./features/ai-chat/ai-chat-page.component').then(m => m.AiChatPageComponent)
  },

  // ── Admin Panel ───────────────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },

  // ── Seller Panel ──────────────────────────────────────────────────────────
  {
    path: 'seller',
    canActivate: [authGuard, sellerGuard],
    loadChildren: () => import('./features/seller/seller.routes').then(m => m.sellerRoutes)
  },

  // ── Fallback ──────────────────────────────────────────────────────────────
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
