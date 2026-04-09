import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

function passwordMatch(ctrl: AbstractControl) {
  const p = ctrl.get('password')?.value;
  const c = ctrl.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly serverError = signal('');

  readonly form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordMatch });

  showError(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && (c?.dirty || c?.touched));
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.loading.set(true);
    this.auth.resetPassword({ token, newPassword: this.form.value.password! }).subscribe({
      next: () => {
        this.toast.success('Şifreniz güncellendi. Giriş yapabilirsiniz.');
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.serverError.set(err.error?.message ?? 'Bağlantı geçersiz veya süresi dolmuş.');
      }
    });
  }
}
