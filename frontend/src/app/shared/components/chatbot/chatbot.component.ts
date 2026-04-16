import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiChatService } from '../../../core/services/ai-chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { environment } from '../../../../environments/environment';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss'
})
export class ChatbotComponent implements AfterViewChecked {
  readonly aiChat = inject(AiChatService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);

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
    const products = (productData?.content ?? productData) as import('../../../core/models/product.model').ProductSummary[];
    if (products?.length) {
      this.productService.applyAiFilter(products);
      this.router.navigate(['/products']);
      this.open.set(false); // sohbet penceresini kapat
    }
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
}
