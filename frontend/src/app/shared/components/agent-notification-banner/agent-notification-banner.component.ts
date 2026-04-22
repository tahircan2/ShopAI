import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentBridgeService } from '../../../services/agent-bridge.service';

@Component({
  selector: 'app-agent-notification-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (agentBridge.notificationBanner$()) {
      <div class="agent-banner slide-in">
        <span class="agent-banner-icon">🤖</span>
        <div class="agent-banner-content">
          <h4>İşlem Onayı Bekleniyor</h4>
          <p>AI asistanınız bir işlemi tamamlamak için onayınızı bekliyor.</p>
        </div>
        <button class="btn btn-sm btn-light" (click)="openChat()">Görüntüle</button>
      </div>
    }
  `,
  styles: [`
    .agent-banner {
      position: fixed;
      top: 80px;
      left: 50%;
      transform: translateX(-50%);
      background: var(--color-primary);
      color: white;
      padding: 12px 20px;
      border-radius: var(--radius-md);
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      z-index: 1100;
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .agent-banner-icon { font-size: 24px; }
    .agent-banner-content h4 { margin: 0 0 4px 0; font-size: 14px; }
    .agent-banner-content p { margin: 0; font-size: 12px; opacity: 0.9; }
    .btn-light { background: white; color: var(--color-primary); border: none; padding: 4px 12px; border-radius: var(--radius-sm); cursor: pointer; font-weight: 600; }
    .slide-in { animation: slideIn 0.3s ease-out; }
    @keyframes slideIn { from { top: -50px; opacity: 0; } to { top: 80px; opacity: 1; } }
  `]
})
export class AgentNotificationBannerComponent {
  agentBridge = inject(AgentBridgeService);

  openChat(): void {
    // Kapat banner'ı
    this.agentBridge.notificationBanner$.set(false);
    
    const toggleBtn = document.querySelector('.chat-toggle-btn') as HTMLElement;
    if (toggleBtn) {
      if (toggleBtn.getAttribute('aria-expanded') !== 'true') {
        toggleBtn.click();
      }
    }
  }
}
