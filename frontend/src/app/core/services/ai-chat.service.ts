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
  readonly sessionId = signal(crypto.randomUUID());

  checkInjection(message: string): boolean {
    return INJECTION_PATTERNS.some(p => p.test(message));
  }

  sendMessage(text: string): Observable<ChatResponse> {
    const req: ChatRequest = {
      sessionId: this.sessionId(),
      message: text
      // userId is NOT sent - backend gets it from JWT cookie
    };

    this.isTyping.set(true);
    return this.http.post<ChatResponse>(this.api, req).pipe(
      tap({
        next: (res) => {
          this.isTyping.set(false);
          this.handleAgentAction(res);
        },
        error: () => this.isTyping.set(false)
      })
    );
  }

  getHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${environment.apiUrl}/ai/conversations/${sessionId}`);
  }

  clearHistory(): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/ai/conversations/${this.sessionId()}`).pipe(
      tap(() => { this.messages.set([]); this.sessionId.set(crypto.randomUUID()); })
    );
  }

  addLocalMessage(role: 'user' | 'assistant', content: string): void {
    const msg: ChatMessage = {
      id: Date.now(),
      role,
      content,
      createdAt: new Date().toISOString()
    };
    this.messages.update(ms => [...ms, msg]);
  }

  private handleAgentAction(res: ChatResponse): void {
    if (!res.actionType || !res.actionData) return;

    switch (res.actionType) {
      case 'PRODUCT_LIST': {
        const products = (res.actionData.data as import('../models/product.model').ProductSummary[]);
        if (products?.length) {
          this.productService.applyAiFilter(products);
          this.router.navigate(['/products']);
        }
        break;
      }
      case 'CART_UPDATED': {
        this.cartService.getCart().subscribe();
        break;
      }
      case 'NAVIGATE': {
        const url = res.actionData.data as string;
        if (url) this.router.navigateByUrl(url);
        break;
      }
    }
  }
}
