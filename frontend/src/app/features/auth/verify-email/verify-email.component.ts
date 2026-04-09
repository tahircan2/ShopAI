import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  // 'pending'  → kayıt sonrası "e-postanızı kontrol edin" ekranı
  // 'loading'  → token ile doğrulama yapılıyor
  // 'success'  → başarıyla doğrulandı
  // 'error'    → token geçersiz/süresi dolmuş
  readonly status = signal<'pending' | 'loading' | 'success' | 'error'>('loading');
  readonly email = signal('');
  readonly resendLoading = signal(false);
  readonly resendCountdown = signal(0);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const emailParam = this.route.snapshot.queryParamMap.get('email') ?? '';
    const sent = this.route.snapshot.queryParamMap.get('sent');

    this.email.set(emailParam);

    if (sent === 'true') {
      // Kayıt sonrası yönlendirme — e-posta gönderildi, kullanıcı bekliyor
      this.status.set('pending');
    } else if (token) {
      // E-postadaki linke tıklandı — otomatik doğrula
      this.status.set('loading');
      this.auth.verifyEmail(token).subscribe({
        next: () => this.status.set('success'),
        error: () => this.status.set('error')
      });
    } else {
      this.status.set('error');
    }
  }

  resendEmail(): void {
    if (this.resendCountdown() > 0 || !this.email()) return;
    this.resendLoading.set(true);
    this.auth.resendVerification(this.email()).subscribe({
      next: () => {
        this.resendLoading.set(false);
        this.toast.success('Doğrulama e-postası tekrar gönderildi!');
        this.startCountdown(60);
      },
      error: () => {
        this.resendLoading.set(false);
        this.toast.error('E-posta gönderilemedi. Lütfen tekrar deneyin.');
      }
    });
  }

  private startCountdown(seconds: number): void {
    this.resendCountdown.set(seconds);
    const interval = setInterval(() => {
      const current = this.resendCountdown();
      if (current <= 1) {
        clearInterval(interval);
        this.resendCountdown.set(0);
      } else {
        this.resendCountdown.set(current - 1);
      }
    }, 1000);
  }
}
