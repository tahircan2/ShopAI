import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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
    <app-navbar />
    <main class="page-content">
      <router-outlet />
    </main>
    <app-footer />
    <app-toast />
    <app-chatbot />
    @if (authService.sessionExpired()) {
      <app-session-expired />
    }
    <app-agent-notification-banner />
  `,
  styles: [`:host { display: flex; flex-direction: column; min-height: 100vh; } .page-content { flex: 1; padding-top: 64px; }`]
})
export class AppComponent {
  readonly authService = inject(AuthService);
}
