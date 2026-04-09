import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserRole } from '../../../core/models/user.model';

function passwordMatch(ctrl: AbstractControl) {
  const p = ctrl.get('password')?.value;
  const c = ctrl.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly serverError = signal('');
  readonly showPass = signal(false);
  toggleShowPass(): void { this.showPass.update(v => !v); }
  readonly selectedRole = signal<UserRole>(
    (this.route.snapshot.queryParamMap.get('role') as UserRole) ?? 'USER'
  );

  readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    shopName: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordMatch });

  setRole(role: UserRole): void {
    this.selectedRole.set(role);
    if (role === 'SELLER') {
      this.form.get('shopName')?.setValidators(Validators.required);
    } else {
      this.form.get('shopName')?.clearValidators();
    }
    this.form.get('shopName')?.updateValueAndValidity();
  }

  showError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && (ctrl?.dirty || ctrl?.touched));
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (!ctrl?.errors) return '';
    if (ctrl.errors['required']) return 'Bu alan zorunludur.';
    if (ctrl.errors['email']) return 'Geçerli bir e-posta girin.';
    if (ctrl.errors['minlength']) return 'Şifre en az 8 karakter olmalıdır.';
    return '';
  }

  strengthPct(): number {
    const p = this.form.get('password')?.value ?? '';
    let score = 0;
    if (p.length >= 8) score += 25;
    if (p.length >= 12) score += 15;
    if (/[A-Z]/.test(p)) score += 20;
    if (/[0-9]/.test(p)) score += 20;
    if (/[^a-zA-Z0-9]/.test(p)) score += 20;
    return Math.min(100, score);
  }

  strengthClass(): string {
    const pct = this.strengthPct();
    if (pct < 40) return 'weak';
    if (pct < 75) return 'medium';
    return 'strong';
  }

  strengthLabel(): string {
    const c = this.strengthClass();
    return c === 'weak' ? 'Zayıf şifre' : c === 'medium' ? 'Orta güçte' : 'Güçlü şifre ✓';
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.serverError.set('');

    const v = this.form.value;
    this.auth.register({
      email: v.email!,
      password: v.password!,
      firstName: v.firstName!,
      lastName: v.lastName!,
      phone: v.phone || undefined,
      role: this.selectedRole(),
      shopName: v.shopName || undefined
    }).subscribe({
      next: () => {
        this.router.navigate(['/auth/verify-email'], {
          queryParams: { email: v.email, sent: 'true' }
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.serverError.set(err.error?.message ?? 'Kayıt sırasında hata oluştu.');
      }
    });
  }
}
