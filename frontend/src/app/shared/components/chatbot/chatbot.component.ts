import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiChatService } from '../../../core/services/ai-chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { environment } from '../../../../environments/environment';

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

    this.aiChat.sendMessage(text).subscribe({
      next: (res) => {
        this.aiChat.addLocalMessage('assistant', res.message);
      },
      error: () => {
        this.aiChat.addLocalMessage('assistant', 'Üzgünüm, şu anda yanıt veremiyorum. Lütfen tekrar deneyin.');
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

  private scrollToBottom(): void {
    try {
      const el = this.msgContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {}
  }
}
