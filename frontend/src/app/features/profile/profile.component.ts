import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { AddressResponse, User } from '../../core/models/user.model';
import { Order, OrderSummary } from '../../core/models/product.model';

type Tab = 'profile' | 'orders' | 'addresses' | 'security' | 'reviews';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly orderService = inject(OrderService);
  private readonly toast = inject(ToastService);

  readonly activeTab = signal<Tab>('profile');
  readonly orders = signal<OrderSummary[]>([]);
  readonly addresses = signal<AddressResponse[]>([]);
  readonly reviews = signal<any[]>([]);
  readonly loadingOrders = signal(false);
  readonly loadingReviews = signal(false);
  readonly savingProfile = signal(false);
  readonly savingAddress = signal(false);
  readonly changingPw = signal(false);
  readonly showAddressForm = signal(false);
  toggleAddressForm(): void { this.showAddressForm.update(v => !v); }
  readonly pwError = signal('');

  readonly tabList: { key: Tab; label: string; icon: string }[] = [
    { key: 'profile', label: 'Profil', icon: '👤' },
    { key: 'orders', label: 'Siparişler', icon: '📦' },
    { key: 'addresses', label: 'Adresler', icon: '📍' },
    { key: 'reviews', label: 'Yorumlarım', icon: '⭐' },
    { key: 'security', label: 'Güvenlik', icon: '🔒' }
  ];

  readonly profileForm = this.fb.group({
    firstName: [this.auth.currentUser()?.firstName ?? ''],
    lastName: [this.auth.currentUser()?.lastName ?? ''],
    phone: [this.auth.currentUser()?.phone ?? '']
  });

  readonly addressForm = this.fb.group({
    fullName: ['', Validators.required],
    label: [''],
    addressLine1: ['', Validators.required],
    city: ['', Validators.required],
    postalCode: [''],
    country: ['Türkiye'],
    isDefault: [false]
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit(): void {
    this.loadOrders();
    this.loadAddresses();
    this.loadReviews();
  }

  loadOrders(): void {
    this.loadingOrders.set(true);
    this.orderService.getMyOrders().subscribe({
      next: (res) => { this.orders.set(res.content); this.loadingOrders.set(false); },
      error: () => this.loadingOrders.set(false)
    });
  }

  loadReviews(): void {
    this.loadingReviews.set(true);
    this.userService.getMyReviews().subscribe({
      next: (res) => { this.reviews.set(res.content); this.loadingReviews.set(false); },
      error: () => this.loadingReviews.set(false)
    });
  }

  loadAddresses(): void {
    this.userService.getAddresses().subscribe({
      next: (addrs) => this.addresses.set(addrs)
    });
  }

  updateProfile(): void {
    this.savingProfile.set(true);
    this.userService.updateProfile(this.profileForm.value as Partial<User>).subscribe({
      next: (user) => {
        this.auth.currentUser.set(user);
        this.savingProfile.set(false);
        this.toast.success('Profil güncellendi.');
      },
      error: () => { this.savingProfile.set(false); this.toast.error('Hata oluştu.'); }
    });
  }

  showErr(f: string): boolean {
    const c = this.addressForm.get(f);
    return !!(c?.invalid && (c?.dirty || c?.touched));
  }

  saveAddress(): void {
    if (this.addressForm.invalid) { this.addressForm.markAllAsTouched(); return; }
    this.savingAddress.set(true);
    this.userService.createAddress(this.addressForm.value as AddressResponse).subscribe({
      next: (addr) => {
        this.addresses.update(a => [...a, addr]);
        this.savingAddress.set(false);
        this.showAddressForm.set(false);
        this.addressForm.reset({ country: 'Türkiye', isDefault: false });
        this.toast.success('Adres eklendi.');
      },
      error: () => { this.savingAddress.set(false); this.toast.error('Hata oluştu.'); }
    });
  }

  deleteAddress(id: number): void {
    this.userService.deleteAddress(id).subscribe({
      next: () => { this.addresses.update(a => a.filter(x => x.id !== id)); this.toast.success('Adres silindi.'); },
      error: () => this.toast.error('Hata oluştu.')
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    this.changingPw.set(true);
    this.pwError.set('');
    this.auth.changePassword({
      currentPassword: this.passwordForm.value.currentPassword!,
      newPassword: this.passwordForm.value.newPassword!
    }).subscribe({
      next: () => { this.changingPw.set(false); this.passwordForm.reset(); this.toast.success('Şifre güncellendi.'); },
      error: (err) => { this.changingPw.set(false); this.pwError.set(err.error?.message ?? 'Hata oluştu.'); }
    });
  }

  initials(): string {
    const u = this.auth.currentUser();
    return u ? `${u.firstName[0]}${u.lastName[0]}`.toUpperCase() : '';
  }

  roleBadge(): string {
    const r = this.auth.userRole();
    return r === 'ADMIN' ? 'badge badge-danger' : r === 'SELLER' ? 'badge badge-warning' : 'badge badge-primary';
  }

  roleLabel(): string {
    return this.auth.userRole() === 'ADMIN' ? 'Admin' : this.auth.userRole() === 'SELLER' ? 'Satıcı' : 'Kullanıcı';
  }

  statusLabel(s: string): string {
    const m: Record<string, string> = { PENDING: 'Bekliyor', CONFIRMED: 'Onaylandı', SHIPPED: 'Kargoda', DELIVERED: 'Teslim Edildi', CANCELLED: 'İptal', REFUNDED: 'İade' };
    return m[s] ?? s;
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = { PENDING: 'badge badge-warning', CONFIRMED: 'badge badge-info', SHIPPED: 'badge badge-primary', DELIVERED: 'badge badge-success', CANCELLED: 'badge badge-danger', REFUNDED: 'badge badge-muted' };
    return m[s] ?? 'badge badge-muted';
  }
}
