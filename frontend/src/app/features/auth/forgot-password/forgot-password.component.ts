import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-center">
      <div class="auth-card">
        @if (!sent()) {
          <div class="auth-header">
            <div class="icon-wrap">🔑</div>
            <h1>Şifremi Unuttum</h1>
            <p>E-posta adresinizi girin, sıfırlama bağlantısı gönderelim.</p>
          </div>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-group">
              <label class="form-label">E-posta</label>
              <input formControlName="email" type="email" class="form-input"
                [class.error]="showError()" placeholder="ornek@email.com" />
              @if (showError()) {
                <span class="form-error">Geçerli bir e-posta girin.</span>
              }
            </div>
            @if (serverError()) {
              <div class="server-error">{{ serverError() }}</div>
            }
            <button type="submit" class="btn btn-primary btn-full btn-lg" style="margin-top:20px" [disabled]="loading()">
              @if (loading()) { <span class="spinner"></span> }
              Sıfırlama Bağlantısı Gönder
            </button>
          </form>
          <div style="text-align:center;margin-top:20px">
            <a routerLink="/auth/login" class="back-link">← Giriş sayfasına dön</a>
          </div>
        } @else {
          <div class="success-state">
            <div class="success-icon">📬</div>
            <h2>E-posta Gönderildi!</h2>
            <p>
              <strong>{{ form.value.email }}</strong> adresine sıfırlama bağlantısı gönderdik.
              Gelen kutunuzu kontrol edin.
            </p>
            <a routerLink="/auth/login" class="btn btn-ghost btn-full" style="margin-top:20px">
              Giriş Sayfasına Dön
            </a>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .auth-center {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 80px 24px 40px;
      background: var(--clr-bg);
    }

    .auth-card {
      width: 100%;
      max-width: 400px;
      animation: fade-in 0.4s ease;
    }

    .auth-header {
      margin-bottom: 28px;
      text-align: center;
      .icon-wrap { font-size: 40px; margin-bottom: 16px; }
      h1 { font-size: 1.7rem; margin-bottom: 8px; }
      p { color: var(--clr-text-muted); font-size: 14px; line-height: 1.5; }
    }

    .server-error { margin-top: 14px; padding: 12px 14px; background: rgba(248,113,113,0.1); border: 1px solid rgba(248,113,113,0.2); border-radius: var(--radius-md); color: var(--clr-danger); font-size: 13px; }

    .back-link { font-size: 13px; color: var(--clr-text-muted); &:hover { color: var(--clr-primary-light); } }

    .success-state {
      text-align: center;
      padding: 20px 0;
      .success-icon { font-size: 48px; margin-bottom: 16px; }
      h2 { font-size: 1.4rem; margin-bottom: 12px; }
      p { color: var(--clr-text-muted); font-size: 14px; line-height: 1.6; }
      strong { color: var(--clr-text); }
    }
  `]
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly serverError = signal('');

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  showError(): boolean {
    const c = this.form.get('email');
    return !!(c?.invalid && (c?.dirty || c?.touched));
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.auth.forgotPassword({ email: this.form.value.email! }).subscribe({
      next: () => { this.loading.set(false); this.sent.set(true); },
      error: (err) => { this.loading.set(false); this.serverError.set(err.error?.message ?? 'Hata oluştu.'); }
    });
  }
}
