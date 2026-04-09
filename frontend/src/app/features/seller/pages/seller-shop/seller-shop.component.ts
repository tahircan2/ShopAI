import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-seller-shop',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './seller-shop.component.html',
  styleUrl: './seller-shop.component.scss'
})
export class SellerShopComponent {
  private readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly saved = signal(false);

  readonly form = this.fb.group({
    shopName: [this.auth.currentUser()?.shopName ?? ''],
    shopDescription: [this.auth.currentUser()?.shopDescription ?? ''],
    firstName: [this.auth.currentUser()?.firstName ?? ''],
    lastName: [this.auth.currentUser()?.lastName ?? ''],
    phone: [this.auth.currentUser()?.phone ?? '']
  });

  save(): void {
    this.saving.set(true);
    this.userService.updateProfile(this.form.value as any).subscribe({
      next: user => {
        this.auth.currentUser.set(user);
        this.saving.set(false);
        this.saved.set(true);
        setTimeout(() => this.saved.set(false), 3000);
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Hata oluştu.');
      }
    });
  }
}
