import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly serverError = signal('');
  readonly showPassword = signal(false);

  toggleShowPassword(): void { this.showPassword.update(v => !v); }

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  showError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && (ctrl?.dirty || ctrl?.touched));
  }

  getError(field: string): string {
    const ctrl = this.form.get(field);
    if (!ctrl?.errors) return '';
    if (ctrl.errors['required']) return 'Bu alan zorunludur.';
    if (ctrl.errors['email']) return 'Geçerli bir e-posta girin.';
    if (ctrl.errors['minlength']) return 'Şifre en az 6 karakter olmalıdır.';
    return '';
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.serverError.set('');

    const { email, password } = this.form.value;
    this.auth.login({ email: email!, password: password! }).subscribe({
      next: () => {
        this.toast.success('Hoş geldiniz!');
        let returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        
        if (!returnUrl || returnUrl === '/') {
          const role = this.auth.userRole();
          if (role === 'ADMIN') {
            returnUrl = '/admin';
          } else if (role === 'SELLER') {
            returnUrl = '/seller';
          } else {
            returnUrl = '/';
          }
        }
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading.set(false);
        const msg: string = err.error?.message ?? 'E-posta veya şifre hatalı.';
        // E-posta doğrulanmamışsa verify sayfasına yönlendir (e-posta ile)
        if (msg.toLowerCase().includes('e-posta') && msg.toLowerCase().includes('doğrula')) {
          const email = this.form.value.email ?? '';
          this.router.navigate(['/auth/verify-email'], { queryParams: { email, sent: 'true' } });
        } else {
          this.serverError.set(msg);
        }
      }
    });
  }
}
