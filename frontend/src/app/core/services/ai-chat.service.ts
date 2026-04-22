import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/product.model';
import { ProductService } from './product.service';
import { CartService } from './cart.service';
import { Router } from '@angular/router';

// Prompt injection patterns (Layer 1 - Frontend)
const INJECTION_PATTERNS = [
  /ignore\s+(previous|prior|all)\s+instructions?/i,
  /forget\s+(your\s+)?(instructions?|rules?|guidelines?)/i,
  /system\s*prompt/i,
  /you\s+are\s+now\s+/i,
  /pretend\s+to\s+be/i,
  /act\s+as\s+(if\s+you\s+(are|were)|an?\s+)/i,
  /jailbreak/i,
  /DAN\s+mode/i,
  /override\s+(safety|guidelines?|rules?)/i,
  /show\s+me\s+(all\s+)?(user|admin|database)/i
];

@Injectable({ providedIn: 'root' })
export class AiChatService {
  private readonly http = inject(HttpClient);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly router = inject(Router);
  private readonly api = `${environment.apiUrl}/ai/chat`;

  readonly messages = signal<ChatMessage[]>([]);
  readonly isTyping = signal(false);
  readonly sessionId = signal<string>(this.getOrInitSessionId());

  constructor() {
    this.loadHistoryFromBackend();
  }

  private getOrInitSessionId(): string {
    let id = localStorage.getItem('ai_session_id');
    if (!id) {
      id = crypto.randomUUID();
      localStorage.setItem('ai_session_id', id);
    }
    return id;
  }

  private loadHistoryFromBackend(): void {
    const sid = this.sessionId();
    // Yalnızca giriş yapmış kullanıcılar için geçmiş yüklenebilir (backend kısıtlaması)
    this.getHistory(sid).subscribe({
      next: (res: any) => {
        if (res && res.messages) {
          const mapped = res.messages.map((m: any) => ({
            ...m,
            role: m.role.toLowerCase() as 'user' | 'assistant',
            id: new Date(m.createdAt).getTime()
          }));
          this.messages.set(mapped);
        }
      },
      error: (err) => {
        console.warn('AI history could not be loaded (likely not logged in):', err);
      }
    });
  }

  checkInjection(message: string): boolean {
    return INJECTION_PATTERNS.some(p => p.test(message));
  }

  sendMessageStream(text: string): Observable<any> {
    const req: ChatRequest = {
      sessionId: this.sessionId(),
      message: text
    };

    return new Observable(observer => {
      this.isTyping.set(true);
      let processedIndex = 0;

      const subscription = this.http.post(`${this.api}/stream`, req, {
        headers: { 'X-Silent': 'true' },
        observe: 'events',
        reportProgress: true,
        responseType: 'text'
      }).subscribe({
        next: (event: any) => {
          if (event.type === 2) { // HttpEventType.ResponseHeader
            // Do not clear typing here; wait for actual data!
          } else if (event.type === 3) { // HttpEventType.DownloadProgress
            const partialText = event.partialText || '';
            const newText = partialText.substring(processedIndex);
            const parts = newText.split('\n\n');
            
            if (parts.length > 1) {
              const completeParts = parts.slice(0, parts.length - 1);
              completeParts.forEach((part: string) => {
                processedIndex += part.length + 2;
                const dataLine = part.split('\n').find((line: string) => line.startsWith('data:'));
                if (dataLine) {
                  try {
                    const rawJson = dataLine.replace(/^data:\s*/, '');
                    const parsed = JSON.parse(rawJson);
                    this.isTyping.set(false);
                    observer.next(parsed);
                  } catch (e) {}
                }
              });
            }
          } else if (event.type === 4) { // HttpEventType.Response
            this.isTyping.set(false);
            const bodyText = event.body as string;
            if (processedIndex < bodyText.length) {
              const remaining = bodyText.substring(processedIndex);
              const parts = remaining.split('\n\n');
              parts.forEach((part: string) => {
                const dataLine = part.split('\n').find((line: string) => line.startsWith('data:'));
                if (dataLine) {
                  try {
                    const rawJson = dataLine.replace(/^data:\s*/, '');
                    const parsed = JSON.parse(rawJson);
                    this.isTyping.set(false);
                    observer.next(parsed);
                  } catch (e) {}
                }
              });
            }
            observer.complete();
          }
        },
        error: (err) => observer.error(err)
      });

      return () => subscription.unsubscribe();
    }).pipe(
      tap({ finalize: () => this.isTyping.set(false) })
    );
  }

  getHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${environment.apiUrl}/ai/conversation/${sessionId}`);
  }

  clearHistory(): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/ai/conversation/${this.sessionId()}`).pipe(
      tap(() => { 
        const newId = crypto.randomUUID();
        this.messages.set([]); 
        this.sessionId.set(newId);
        localStorage.setItem('ai_session_id', newId);
      })
    );
  }

  addLocalMessage(
    role: 'user' | 'assistant', 
    content: string, 
    actionType?: import('../models/product.model').AgentActionType,
    actionData?: any,
    isInjectionDetected?: boolean
  ): void {
    const msg: ChatMessage = {
      id: Date.now(),
      role,
      content,
      actionType,
      actionData,
      isInjectionDetected,
      createdAt: new Date().toISOString()
    };
    this.messages.update(ms => [...ms, msg]);
  }

  handleAgentAction(res: any): void {
    if (!res.actionType || !res.actionData) return;

    switch (res.actionType) {
      case 'PRODUCT_LIST': {
        // Otomatik yönlendirmeyi kaldırıp chat ekranında göstereceğiz
        // chatbot component'i mesajın actionType'ı sayesinde ürünleri listeleyecek
        break;
      }
      case 'CART_UPDATED': {
        this.cartService.getCart().subscribe();
        break;
      }
      case 'NAVIGATE': {
        const url = res.actionData?.path as string;
        if (url) this.router.navigateByUrl(url);
        break;
      }
      case 'ORDER_INFO': {
        // Sipariş bilgisi — chatbot'ta mesaj olarak gösterilir, ekstra aksiyon gerekmez
        break;
      }
      case 'APPROVAL_REQUIRED': {
        // Onay kartı — chatbot component tarafından render edilir (actionData.approvalToken)
        // Sepeti de güncelle (kullanıcının sepeti checkout aşamasında olabilir)
        this.cartService.getCart().subscribe();
        break;
      }
      case 'STEP_PROGRESS': {
        // Progress stepper — chatbot component tarafından render edilir (actionData.transactionId)
        break;
      }
      case 'CHECKOUT_COMPLETE': {
        // Sipariş tamamlandı — sepeti güncelle (artık boş) ve bildirim göster
        this.cartService.getCart().subscribe();
        break;
      }
      case 'AI_FEEDBACK_REQUEST': {
        // Feedback istendi — chatbot component'i render eder
        break;
      }
    }
  }
}
