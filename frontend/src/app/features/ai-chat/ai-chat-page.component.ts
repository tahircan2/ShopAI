import { Component, inject, signal, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiChatService } from '../../core/services/ai-chat.service';
import { AuthService } from '../../core/services/auth.service';
import { MarkdownPipe } from '../../shared/pipes/markdown.pipe';
import { CurrencyFormatPipe } from '../../shared/pipes/shared-pipes';
import { AgentApprovalCardComponent } from '../../shared/components/agent-approval-card/agent-approval-card.component';
import { AgentProgressComponent } from '../../shared/components/agent-progress/agent-progress.component';
import { ChatDataTableComponent } from '../../shared/components/chat-data-table/chat-data-table.component';
import { Router, RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-ai-chat-page',
  standalone: true,
  imports: [CommonModule, FormsModule, MarkdownPipe, CurrencyFormatPipe, AgentApprovalCardComponent, AgentProgressComponent, ChatDataTableComponent, DatePipe, RouterLink],
  templateUrl: './ai-chat-page.component.html',
  styleUrl: './ai-chat-page.component.scss'
})
export class AiChatPageComponent implements OnInit, AfterViewChecked {
  readonly aiChat = inject(AiChatService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('chatContainer') chatContainer!: ElementRef<HTMLDivElement>;

  inputText = '';
  sidebarOpen = false;
  showAllSessions = false;
  showConfirmModal = false;
  private shouldScrollBottom = false;

  get displayedSessions() {
    const sessions = this.aiChat.conversations();
    if (this.showAllSessions) return sessions;
    return sessions.slice(0, 3);
  }

  get quickQuestions(): string[] {
    const role = this.auth.currentUser()?.role;
    if (role === 'ADMIN') {
      return [
        '📊 Platform geneli gelir trendi',
        '👥 Kullanıcı dağılımı',
        '🏪 En çok satan mağazalar',
        '📦 Tüm bekleyen siparişler',
        '🔍 Son audit logları'
      ];
    }
    if (role === 'SELLER') {
      return [
        '📊 Bu haftaki satışlarım',
        '🏆 En çok satan 5 ürünüm',
        '📦 Bekleyen siparişlerim',
        '⭐ Ürünlerimin puan dağılımı',
        '🛍️ Kategoriye göre satışlarım'
      ];
    }
    return [
      '📱 En popüler telefonlar',
      '🎧 Kulaklık önerir misin?',
      '🛒 Sepetimde ne var?',
      '📦 Son siparişlerim',
      '❓ Kargo süresi ne kadar?'
    ];
  }

  ngOnInit(): void {
    this.shouldScrollBottom = true;
    // Explicitly refresh history when entering the page
    if (this.auth.isLoggedIn()) {
      this.aiChat.loadHistoryFromBackend();
      this.aiChat.loadSessions();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollBottom) {
      this.scrollToBottom();
      this.shouldScrollBottom = false;
    }
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.aiChat.isTyping()) return;
    this.inputText = '';
    this.shouldScrollBottom = true;

    // Add user message immediately
    this.aiChat.addLocalMessage('user', text);

    let accumulatedText = '';
    const botMsgId = Date.now() + 1;

    this.aiChat.sendMessageStream(text).subscribe({
      next: (chunk: any) => {
        this.shouldScrollBottom = true;
        if (chunk.type === 'token') {
          accumulatedText += chunk.content;
          // Update or create bot message with streaming text
          const msgs = this.aiChat.messages();
          const existing = msgs.find((m: any) => m.id === botMsgId);
          if (existing) {
            existing.content = accumulatedText;
            this.aiChat.messages.set([...msgs]);
          } else {
            this.aiChat.messages.update(ms => [...ms, {
              id: botMsgId, role: 'assistant' as const, content: accumulatedText,
              createdAt: new Date().toISOString()
            }]);
          }
        } else if (chunk.type === 'state' && chunk.state) {
          const state = chunk.state;
          const msgs = this.aiChat.messages();
          const existing = msgs.find((m: any) => m.id === botMsgId);
          const finalContent = accumulatedText || state.message || '';

          if (existing) {
            existing.content = finalContent;
            existing.actionType = state.action_type || state.actionType;
            existing.actionData = state.action_data || state.actionData;
            existing.isInjectionDetected = state.injection_detected || state.injectionDetected;
            this.aiChat.messages.set([...msgs]);
          } else {
            this.aiChat.addLocalMessage(
              'assistant', finalContent,
              state.action_type || state.actionType,
              state.action_data || state.actionData,
              state.injection_detected || state.injectionDetected
            );
          }
          this.aiChat.handleAgentAction(state);
        }
      },
      error: () => {
        this.aiChat.addLocalMessage('assistant', 'Bir hata oluştu. Lütfen tekrar deneyin.');
      }
    });
  }

  sendQuick(q: string): void {
    this.inputText = q;
    this.send();
  }

  switchSession(sid: string): void {
    if (sid === this.aiChat.sessionId()) return;
    this.aiChat.switchToSession(sid);
    this.shouldScrollBottom = true;
  }

  newChat(): void {
    this.aiChat.clearChat();
    this.shouldScrollBottom = true;
  }

  clearChat(): void {
    this.showConfirmModal = true;
  }

  confirmClearChat(): void {
    this.showConfirmModal = false;
    this.aiChat.clearHistory().subscribe({
      next: () => {
        this.aiChat.loadSessions();
        this.newChat();
      }
    });
  }

  cancelClearChat(): void {
    this.showConfirmModal = false;
  }

  getTop3Products(data: any): any[] {
    if (!data?.products) return [];
    return data.products.slice(0, 3);
  }

  hasMoreProducts(data: any): boolean {
    return data?.products?.length > 3;
  }

  goToProductDetail(slug: string): void {
    this.router.navigate(['/products', slug]);
  }

  onApprovalCompleted(messageId: string, event: any): void {
    const messages = this.aiChat.messages();
    const msg = messages.find((m: any) => m.id === messageId);
    if (msg) {
      msg.actionType = undefined;
      if (event.success) {
        msg['feedbackRequested'] = true;
      }
    }
  }

  onProgressComplete(msg: any, event: any): void {
    msg['feedbackRequested'] = true;
  }

  submitFeedback(msg: any, rating: number): void {
    msg['feedbackGiven'] = true;
    msg['feedbackRequested'] = false;
  }

  private scrollToBottom(): void {
    if (this.chatContainer?.nativeElement) {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    }
  }

  // ── Analytics Chart Rendering ──────────────────────────────────────────────
  private analyticsCharts = new Map<string, Chart>();

  renderAnalyticsChart(msgId: string, chartConfig: any): void {
    if (!chartConfig) return;

    setTimeout(() => {
      const canvasId = `analytics-chart-${msgId}`;
      const canvas = document.getElementById(canvasId) as HTMLCanvasElement;
      if (!canvas) return;

      // Destroy existing chart if re-rendering
      const existing = this.analyticsCharts.get(msgId);
      if (existing) existing.destroy();

      // Apply dark theme defaults
      const config = { ...chartConfig };
      if (!config.options) config.options = {};
      if (!config.options.plugins) config.options.plugins = {};
      if (!config.options.plugins.legend) config.options.plugins.legend = {};
      config.options.plugins.legend.labels = { ...config.options.plugins.legend.labels, color: '#e2e8f0' };
      config.options.responsive = true;
      config.options.maintainAspectRatio = false;

      if (config.options.scales) {
        for (const axis of Object.values(config.options.scales) as any[]) {
          if (axis.ticks) axis.ticks.color = '#94a3b8';
          if (axis.grid) axis.grid.color = 'rgba(99,102,241,0.08)';
        }
      }

      const chart = new Chart(canvas, config);
      this.analyticsCharts.set(msgId, chart);
    }, 200);
  }

  isAnalyticsResult(msg: any): boolean {
    return msg.actionType === 'ANALYTICS_RESULT' && msg.actionData?.chartConfig;
  }

  onAnalyticsRendered(msg: any): void {
    if (!msg['_chartRendered'] && this.isAnalyticsResult(msg)) {
      msg['_chartRendered'] = true;
      this.renderAnalyticsChart(msg.id, msg.actionData.chartConfig);
    }
  }
}
