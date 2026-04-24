import { Component, inject, signal, computed } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ToastComponent } from './shared/components/toast/toast.component';
import { SessionExpiredComponent } from './shared/components/session-expired/session-expired.component';
import { ChatbotComponent } from './shared/components/chatbot/chatbot.component';
import { LoadingBarComponent } from './shared/components/loading-spinner/loading-bar.component';
import { AgentNotificationBannerComponent } from './shared/components/agent-notification-banner/agent-notification-banner.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    NavbarComponent,
    FooterComponent,
    ToastComponent,
    SessionExpiredComponent,
    ChatbotComponent,
    LoadingBarComponent,
    AgentNotificationBannerComponent
  ],
  template: `
    <app-loading-bar />
    @if (!isAiChat()) {
      <app-navbar />
    }
    <main class="page-content" [class.no-padding]="isAiChat()">
      <router-outlet />
    </main>
    @if (!isAiChat()) {
      <app-footer />
      <app-chatbot />
    }
    <app-toast />
    @if (authService.sessionExpired()) {
      <app-session-expired />
    }
    <app-agent-notification-banner />
  `,
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; } 
    .page-content { flex: 1; padding-top: 64px; }
    .page-content.no-padding { padding-top: 0; }
  `]
})
export class AppComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUrl = signal<string>('');
  readonly isAiChat = computed(() => this.currentUrl().includes('/ai-chat'));

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.currentUrl.set(event.urlAfterRedirects);
    });
  }
}
