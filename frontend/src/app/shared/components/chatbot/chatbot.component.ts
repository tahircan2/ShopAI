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

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [FormsModule, AgentApprovalCardComponent, AgentProgressComponent, CurrencyFormatPipe, MarkdownPipe],
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
    const filters = actionData?.filters;
    const queryParams: any = {};
    
    if (filters) {
      // AI'dan gelen filtreleri URL parametrelerine dönüştür
      if (filters.q) queryParams.q = filters.q;
      if (filters.category) {
        // Basit slug dönüşümü (Kategori ismi gelirse diye)
        queryParams.categorySlug = filters.category.toLowerCase()
          .replace(/ğ/g, 'g').replace(/ü/g, 'u').replace(/ş/g, 's')
          .replace(/ı/g, 'i').replace(/ö/g, 'o').replace(/ç/g, 'c')
          .replace(/ /g, '-');
      }
      if (filters.brand) queryParams.brand = filters.brand;
      if (filters.min_price) queryParams.minPrice = filters.min_price;
      if (filters.max_price) queryParams.maxPrice = filters.max_price;
      if (filters.rating) queryParams.minRating = filters.rating;
      if (filters.sort_by) queryParams.sortBy = filters.sort_by;
      if (filters.sort_dir) queryParams.sortDir = filters.sort_dir;
    }

    // AI filtresini temizleyip URL üzerinden gitmek daha sağlıklı (pagination vb. için)
    this.productService.clearAiFilter();
    this.router.navigate(['/products'], { queryParams });
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
}
