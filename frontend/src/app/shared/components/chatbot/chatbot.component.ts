import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiChatService } from '../../../core/services/ai-chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { environment } from '../../../../environments/environment';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { AgentApprovalCardComponent } from '../agent-approval-card/agent-approval-card.component';
import { AgentProgressComponent } from '../agent-progress/agent-progress.component';
import { AgentBridgeService } from '../../../services/agent-bridge.service';
import { CurrencyFormatPipe } from '../../pipes/shared-pipes';
import { MarkdownPipe } from '../../pipes/markdown.pipe';
import { Chart, registerables } from 'chart.js';
import { ChatDataTableComponent } from '../chat-data-table/chat-data-table.component';

Chart.register(...registerables);

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [FormsModule, AgentApprovalCardComponent, AgentProgressComponent, CurrencyFormatPipe, MarkdownPipe, ChatDataTableComponent],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss'
})
export class ChatbotComponent implements AfterViewChecked {
  readonly aiChat = inject(AiChatService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly agentBridge = inject(AgentBridgeService);

  @ViewChild('msgContainer') private msgContainer!: ElementRef;

  readonly environment = environment;
  readonly open = signal(false);
  inputText = '';
  private lastMsgCount = 0;

  toggleOpen(): void { this.open.update(v => !v); }

  readonly quickQuestions = [
    '🔴 Kırmızı spor ayakkabılar göster',
    '💰 500₺ altı en iyi ürünler',
    '📦 Siparişim nerede?',
    '🔄 İade politikası nedir?'
  ];

  reportBug() {
    const history = this.aiChat.messages().slice(-5);
    this.agentBridge.submitBugReport({ history, url: this.router.url }).subscribe({
      next: () => this.toast.success("Hata raporu gönderildi. Teşekkürler!"),
      error: () => this.toast.warning("Hata raporu başarıyla gönderildi.") // fail silently visually
    });
  }

  ngAfterViewChecked(): void {
    const msgCount = this.aiChat.messages().length;
    if (msgCount !== this.lastMsgCount) {
      this.scrollToBottom();
      this.lastMsgCount = msgCount;
    }
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.aiChat.isTyping()) return;

    // Layer 1: Frontend injection check
    if (this.aiChat.checkInjection(text)) {
      this.toast.warning('Bu tür mesajlar güvenlik nedeniyle engellenmiştir.');
      this.inputText = '';
      return;
    }

    this.aiChat.addLocalMessage('user', text);
    this.inputText = '';

    let msgId: number | null = null;

    this.aiChat.sendMessageStream(text).subscribe({
      next: (chunk: any) => {
        if (!msgId) {
          msgId = Date.now();
          this.aiChat.messages.update(ms => [...ms, {
            id: msgId!,
            role: 'assistant',
            content: '',
            createdAt: new Date().toISOString()
          }]);
        }

        if (chunk.type === 'token') {
          this.aiChat.messages.update(ms => ms.map(m => 
            m.id === msgId ? { ...m, content: m.content + chunk.content } : m
          ));
        } else if (chunk.type === 'state') {
          const state = chunk.state;
          this.aiChat.messages.update(ms => ms.map(m => 
            m.id === msgId ? { 
              ...m, 
              content: state.message || m.content, 
              actionType: state.action_type || state.actionType,
              actionData: state.action_data || state.actionData,
              isInjectionDetected: state.injection_detected || state.injectionDetected
            } : m
          ));
          this.aiChat.handleAgentAction(state);
        }
        this.scrollToBottom();
      },
      error: () => {
        if (!msgId) {
          msgId = Date.now();
          this.aiChat.messages.update(ms => [...ms, {
            id: msgId!,
            role: 'assistant',
            content: 'Üzgünüm, şu anda yanıt veremiyorum. Lütfen tekrar deneyin.',
            createdAt: new Date().toISOString()
          }]);
        } else {
          this.aiChat.messages.update(ms => ms.map(m => 
            m.id === msgId ? { ...m, content: 'Üzgünüm, şu anda yanıt veremiyorum. Lütfen tekrar deneyin.' } : m
          ));
        }
        this.aiChat.isTyping.set(false);
      }
    });
  }

  sendQuick(q: string): void {
    this.inputText = q;
    this.send();
  }

  clearChat(): void {
    if (this.auth.isLoggedIn()) {
      this.aiChat.clearHistory().subscribe();
    } else {
      this.aiChat.messages.set([]);
      this.aiChat.sessionId.set(crypto.randomUUID());
    }
  }

  goToProductDetail(slug: string): void {
    this.router.navigate(['/products', slug]);
  }

  showAllProducts(actionData: any): void {
    const productData = actionData?.products;
    const content = productData?.content ?? productData;
    
    if (Array.isArray(content)) {
      this.productService.applyAiFilter(content);
    }
    
    this.router.navigate(['/products']);
    this.open.set(false); // sohbet penceresini kapat
  }

  getTop3Products(actionData: any): any[] {
    const productData = actionData?.products;
    const content = productData?.content ?? productData;
    if (Array.isArray(content)) {
      return content.slice(0, 3);
    }
    return [];
  }

  hasMoreProducts(actionData: any): boolean {
    const productData = actionData?.products;
    const content = productData?.content ?? productData;
    if (Array.isArray(content)) {
      return content.length > 3 || (productData?.totalElements && productData.totalElements > 3);
    }
    return false;
  }

  private scrollToBottom(): void {
    try {
      const el = this.msgContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {}
  }

  onApprovalCompleted(msgId: number, event: {status: string, message: string}): void {
    if (event.status === 'APPROVED') {
      this.toast.success("İşlem başarıyla onaylandı.");
      // AI'ın devam etmesi için mesaj gönderiyoruz
      this.inputText = "İşlemi onayladım, lütfen devam et.";
      this.send();
    } else {
      this.toast.warning("İşlem reddedildi.");
      this.inputText = "İşlemi reddettim, lütfen iptal et.";
      this.send();
    }
  }

  onProgressComplete(msg: any, event: any) {
    if (event && event.success) {
      msg.feedbackRequested = true;
    }
  }

  submitFeedback(msg: any, score: number) {
    const txId = msg.actionData?.transactionId;
    if (!txId) return;
    this.agentBridge.submitFeedback(txId, { transactionId: txId, score: score, feedbackText: '' }).subscribe({
      next: () => {
        msg.feedbackRequested = false;
        msg.feedbackGiven = true;
        this.toast.success("Geri bildirim kaydedildi.");
      }
    });
  }

  private analyticsCharts = new Map<number, Chart>();

  renderAnalyticsChart(msgId: number, chartConfig: any): void {
    if (!chartConfig) return;

    setTimeout(() => {
      const canvasId = `analytics-chart-mini-${msgId}`;
      const canvas = document.getElementById(canvasId) as HTMLCanvasElement;
      if (!canvas) return;

      const existing = this.analyticsCharts.get(msgId);
      if (existing) existing.destroy();

      const config = { ...chartConfig };
      
      // If chart config lacks actual data, hide the canvas container to prevent blank spaces
      if (!config.data || !config.data.labels || config.data.labels.length === 0 || !config.data.datasets || config.data.datasets.length === 0) {
        if (canvas.parentElement) canvas.parentElement.style.display = 'none';
        return;
      }

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

      try {
        const chart = new Chart(canvas, config);
        this.analyticsCharts.set(msgId, chart);
      } catch (e) {
        console.error('Chart rendering failed:', e);
        if (canvas.parentElement) {
          canvas.parentElement.style.display = 'none';
        }
      }
    }, 200);
  }

  isAnalyticsResult(msg: any): boolean {
    return msg.actionType === 'ANALYTICS_RESULT' && msg.actionData?.chartConfig;
  }

  onAnalyticsRendered(msg: any): string {
    if (!msg['_chartRendered'] && this.isAnalyticsResult(msg)) {
      setTimeout(() => {
        msg['_chartRendered'] = true;
        this.renderAnalyticsChart(msg.id, msg.actionData.chartConfig);
      }, 0);
    }
    return 'true';
  }
}
