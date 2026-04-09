// ─── User Models ────────────────────────────────────────────────────────────

export type UserRole = 'USER' | 'ADMIN' | 'SELLER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: UserRole;
  isActive: boolean;
  isEmailVerified: boolean;
  avatarUrl?: string;
  createdAt: string;
  updatedAt?: string;
  // Seller specific
  shopName?: string;
  shopDescription?: string;
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role?: UserRole;
  // Seller specific
  shopName?: string;
  shopDescription?: string;
}

export interface AuthResponse {
  expiresIn: number;
  user: User;
  // No token here - it's in HttpOnly cookie
}

export interface SessionStatus {
  loggedIn: boolean;
  user: User | null;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface AddressRequest {
  label?: string;
  fullName: string;
  phone?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  district?: string;
  postalCode?: string;
  country: string;
  isDefault: boolean;
}

export interface AddressResponse extends AddressRequest {
  id: number;
}

// ─── API Response Types ──────────────────────────────────────────────────────

export interface ApiError {
  status: number;
  message: string;
  errors?: Record<string, string>;
  expired?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ─── Notification ────────────────────────────────────────────────────────────

export type NotificationType = 'ORDER_STATUS' | 'PROMOTION' | 'SYSTEM';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  referenceId?: number;
  createdAt: string;
}
