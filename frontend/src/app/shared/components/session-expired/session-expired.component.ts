import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-session-expired',
  standalone: true,
  template: `
    <div class="overlay">
      <div class="modal">
        <div class="icon-wrap">
          <div class="lock-icon">🔒</div>
        </div>
        <h2>Oturum Sona Erdi</h2>
        <p class="message">
          Güvenliğiniz için oturumunuz sona erdirildi.
        </p>
        <div class="countdown-wrap">
          <div class="countdown" [class.pulse]="countdown() <= 2">
            {{ countdown() }}
          </div>
          <div class="countdown-label">saniye içinde yönlendiriliyorsunuz</div>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" [style.width.%]="progressWidth()"></div>
        </div>
        <button class="btn btn-primary btn-full" (click)="goLogin()">
          Şimdi Giriş Yap
        </button>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.85);
      backdrop-filter: blur(6px);
      z-index: 9000;
      display: flex;
      align-items: center;
      justify-content: center;
      animation: fade-in 0.3s ease;
    }

    .modal {
      background: var(--clr-surface);
      border: 1px solid var(--clr-border);
      border-radius: var(--radius-xl);
      padding: 40px 36px;
      max-width: 360px;
      width: 100%;
      text-align: center;
      box-shadow: var(--shadow-lg);
      animation: slide-up 0.3s ease;
    }

    .icon-wrap {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: rgba(248,113,113,0.1);
      border: 1px solid rgba(248,113,113,0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 24px;
    }

    .lock-icon { font-size: 32px; }

    h2 { font-size: 1.4rem; margin-bottom: 10px; }

    .message { color: var(--clr-text-muted); font-size: 14px; margin-bottom: 28px; }

    .countdown-wrap { margin-bottom: 20px; }

    .countdown {
      font-size: 3.5rem;
      font-weight: 600;
      color: var(--clr-danger);
      font-family: var(--font-mono);
      line-height: 1;
      margin-bottom: 8px;
      transition: transform 0.1s;
      &.pulse { animation: count-pulse 0.5s ease infinite; }
    }

    .countdown-label { font-size: 13px; color: var(--clr-text-muted); }

    .progress-bar {
      height: 4px;
      background: var(--clr-surface-3);
      border-radius: 2px;
      overflow: hidden;
      margin-bottom: 24px;
    }

    .progress-fill {
      height: 100%;
      background: var(--clr-danger);
      border-radius: 2px;
      transition: width 1s linear;
    }
  `]
})
export class SessionExpiredComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly countdown = signal(5);
  readonly progressWidth = () => (this.countdown() / 5) * 100;

  private timer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    const returnUrl = this.router.url;

    this.timer = setInterval(() => {
      this.countdown.update(c => c - 1);
      if (this.countdown() <= 0) {
        this.goLogin(returnUrl);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  goLogin(returnUrl?: string): void {
    if (this.timer) clearInterval(this.timer);
    this.authService.sessionExpired.set(false);
    this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl: returnUrl ?? this.router.url }
    });
  }
}
