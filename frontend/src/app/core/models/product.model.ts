// ─── Product Models ──────────────────────────────────────────────────────────

export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  parentId?: number;
  imageUrl?: string;
  isActive: boolean;
  sortOrder: number;
  children?: Category[];
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  altText?: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface ProductVariant {
  id: number;
  color?: string;
  colorHex?: string;
  size?: string;
  skuVariant: string;
  stockQuantity: number;
  priceModifier: number;
}

export interface Review {
  id: number;
  userId: number;
  userName: string;
  userFullName: string;
  rating: number;
  title?: string;
  comment?: string;
  isVerifiedPurchase: boolean;
  helpfulCount: number;
  createdAt: string;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description?: string;
  longDescription?: string;
  price: number;
  discountedPrice?: number;
  effectivePrice?: number;
  stockQuantity: number;
  sku: string;
  category?: Category;
  brand?: string;
  ratingAvg: number;
  ratingCount: number;
  images: ProductImage[];
  variants: ProductVariant[];
  tags?: string[];
  isFeatured: boolean;
  isActive: boolean;
  metaTitle?: string;
  metaDescription?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ProductSummary {
  id: number;
  name: string;
  slug: string;
  sku?: string;
  description?: string;
  price: number;
  discountedPrice?: number;
  effectivePrice: number;
  stockQuantity: number;
  brand?: string;
  ratingAvg: number;
  ratingCount: number;
  isFeatured: boolean;
  isActive?: boolean;
  primaryImageUrl?: string;
  sellerId?: number;
  sellerName?: string;
  // Kategori bilgileri
  categoryId?: number;
  categoryName?: string;
  categorySlug?: string;
  hasVariants?: boolean;
}

export interface ProductFilter {
  categorySlug?: string;
  minPrice?: number;
  maxPrice?: number;
  colors?: string[];
  sizes?: string[];
  brand?: string;
  rating?: number;
  sortBy?: 'price' | 'ratingAvg' | 'ratingCount' | 'createdAt' | 'name';
  sortDir?: 'asc' | 'desc';
  page?: number;
  size?: number;
  q?: string;
}

export interface ProductPage {
  content: ProductSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ReviewRequest {
  rating: number;
  title?: string;
  comment?: string;
}

// ─── Cart Models ─────────────────────────────────────────────────────────────

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productSlug: string;
  primaryImageUrl?: string;
  variantId?: number;
  variantColor?: string;
  variantSize?: string;
  quantity: number;
  priceAtAdd: number;
  currentPrice: number;
  lineTotal: number;
}

export interface Cart {
  id: number;
  items: CartItem[];
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  total: number;
  appliedCoupon?: string;
  freeShipping: boolean;
}

export interface AddToCartRequest {
  productId: number;
  variantId?: number;
  quantity: number;
}

export interface ApplyCouponRequest {
  code: string;
}

// ─── Order Models ─────────────────────────────────────────────────────────────

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productSku?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  primaryImageUrl?: string;
}

export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  totalAmount: number;
  items: OrderItem[];
  shippingAddress: import('./user.model').AddressResponse;
  paymentStatus: PaymentStatus;
  paymentMethod?: string;
  couponCode?: string;
  notes?: string;
  shippedAt?: string;
  deliveredAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface OrderSummary {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
  // Included in admin response
  userName?: string;
}

export interface CreateOrderRequest {
  shippingAddressId: number;
  notes?: string;
  paymentMethod: string;
}

// ─── AI Chat Models ──────────────────────────────────────────────────────────

export type AgentActionType = 'PRODUCT_LIST' | 'CART_UPDATED' | 'NAVIGATE' | 'INFO' | 'ORDER_INFO' | 'ERROR';
export type MessageRole = 'user' | 'assistant' | 'system';

export interface AgentActionResult {
  type: AgentActionType;
  data: unknown;
  message: string;
}

export interface ChatMessage {
  id: number;
  role: MessageRole;
  content: string;
  agentType?: string;
  actionType?: AgentActionType;
  actionData?: any;
  isInjectionDetected?: boolean;
  createdAt: string;
}

export interface ChatRequest {
  sessionId: string;
  message: string;
  // userId is NOT sent - backend gets it from JWT cookie
}

export interface ChatResponse {
  message: string;
  agentType?: string;
  actionType?: AgentActionType;
  actionData?: any;  // Python her agent'tan farklı yapıda data döner
  injectionDetected?: boolean;
  sessionId?: string;
  intent?: string;
}

// ─── Wishlist ────────────────────────────────────────────────────────────────

export interface WishlistItem extends ProductSummary {
  addedAt?: string;
}

// ─── Seller Models ───────────────────────────────────────────────────────────

export interface SellerStats {
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  monthlyRevenue: number;
  avgRating: number;
}

// ─── Admin Models ─────────────────────────────────────────────────────────────

export interface AdminStats {
  totalUsers: number;
  totalOrders: number;
  totalRevenue: number;
  totalProducts: number;
  pendingOrders: number;
  newUsersThisMonth: number;
}

export interface AuditLog {
  id: number;
  userId?: number;
  action: string;
  entityType?: string;
  entityId?: number;
  oldData?: unknown;
  newData?: unknown;
  ipAddress?: string;
  createdAt: string;
}

export interface Coupon {
  id: number;
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED';
  discountValue: number;
  minOrderAmount?: number;
  maxUses?: number;
  usedCount: number;
  validFrom: string;
  validUntil: string;
  isActive: boolean;
}

export interface CouponRequest {
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED';
  discountValue: number;
  minOrderAmount?: number | null;
  maxUses?: number | null;
  validFrom: string;
  validUntil: string;
}
