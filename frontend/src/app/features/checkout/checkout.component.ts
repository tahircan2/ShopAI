import { Component, inject, signal, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { AddressResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);

  readonly currentStep = signal(0);
  readonly addresses = signal<AddressResponse[]>([]);
  readonly selectedAddressId = signal<number | null>(null);
  readonly loadingAddresses = signal(true);
  readonly loading = signal(false);
  readonly serverError = signal('');
  readonly showAddressForm = signal(false);
  readonly savingAddress = signal(false);

  readonly steps = [
    { num: 1, label: 'Adres' },
    { num: 2, label: 'Özet' },
    { num: 3, label: 'Ödeme' }
  ];

  readonly paymentForm = this.fb.group({
    method: ['CREDIT_CARD'],
    cardName: [''],
    cardNumber: [''],
    expiry: [''],
    cvv: ['']
  });

  readonly addressForm = this.fb.group({
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    addressLine1: ['', Validators.required],
    addressLine2: [''],
    city: ['', Validators.required],
    district: ['', Validators.required],
    postalCode: [''],
    country: ['Türkiye', Validators.required],
    isDefault: [true]
  });

  ngOnInit(): void {
    if (!this.cartService.cart()) {
      this.cartService.getCart().subscribe();
    }
    this.userService.getAddresses().subscribe({
      next: (addrs) => {
        this.addresses.set(addrs);
        const def = addrs.find(a => a.isDefault);
        if (def) this.selectedAddressId.set(def.id);
        this.loadingAddresses.set(false);
      },
      error: () => this.loadingAddresses.set(false)
    });
  }

  nextStep(): void { this.currentStep.update(s => Math.min(s + 1, 2)); }
  prevStep(): void { this.currentStep.update(s => Math.max(s - 1, 0)); }

  saveAddress(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }
    this.savingAddress.set(true);
    this.userService.createAddress(this.addressForm.value as any).subscribe({
      next: (addr) => {
        this.addresses.update(a => [...a, addr]);
        this.selectedAddressId.set(addr.id);
        this.showAddressForm.set(false);
        this.savingAddress.set(false);
        this.addressForm.reset({ country: 'Türkiye', isDefault: true });
        this.toast.success('Adres başarıyla eklendi.');
      },
      error: () => {
        this.savingAddress.set(false);
        this.toast.error('Adres eklenirken hata oluştu.');
      }
    });
  }

  placeOrder(): void {
    const addrId = this.selectedAddressId();
    if (!addrId) { this.toast.error('Adres seçin.'); return; }
    this.loading.set(true);
    this.serverError.set('');

    this.orderService.createOrder({
      shippingAddressId: addrId,
      paymentMethod: this.paymentForm.value.method ?? 'CREDIT_CARD'
    }).subscribe({
      next: (order) => {
        this.loading.set(false);
        this.cartService.cart.set(null);
        this.toast.success('Siparişiniz oluşturuldu!');
        this.router.navigate(['/order-confirmation', order.orderNumber]);
      },
      error: (err) => {
        this.loading.set(false);
        this.serverError.set(err.error?.message ?? 'Sipariş oluşturulamadı.');
      }
    });
  }
}
