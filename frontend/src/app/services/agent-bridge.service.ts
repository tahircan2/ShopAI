import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { 
  ApprovalResponse, 
  ApprovalActionResponse, 
  TransactionResponse,
  QuickCheckoutValidationResponse,
  QuickCheckoutResponse,
  ApplicableCoupon,
  AiPreferenceResponse
} from '../models/AgentResponse.model';
import { 
  QuickCheckoutValidateRequest, 
  QuickCheckoutExecuteRequest,
  UpdateAiPreferenceRequest,
  AgentFeedbackRequest
} from '../models/AgentRequest.model';

@Injectable({
  providedIn: 'root'
})
export class AgentBridgeService {
  private apiUrl = environment.apiUrl;

  readonly pendingApproval$ = signal<any | null>(null);
  readonly transactionProgress$ = signal<any | null>(null);
  readonly notificationBanner$ = signal<boolean>(false);

  constructor(private http: HttpClient) {}

  // --- Approvals ---
  getPendingApprovals(): Observable<ApprovalResponse[]> {
    return this.http.get<ApprovalResponse[]>(`${this.apiUrl}/agent/approvals/pending`);
  }

  getApprovalStatus(token: string): Observable<ApprovalResponse> {
    return this.http.get<ApprovalResponse>(`${this.apiUrl}/agent/approvals/${token}/status`);
  }

  approveAction(token: string): Observable<ApprovalActionResponse> {
    return this.http.post<ApprovalActionResponse>(`${this.apiUrl}/agent/approvals/${token}/approve`, {});
  }

  rejectAction(token: string): Observable<ApprovalActionResponse> {
    return this.http.post<ApprovalActionResponse>(`${this.apiUrl}/agent/approvals/${token}/reject`, {});
  }

  // --- Transactions ---
  getTransactions(page: number = 0, size: number = 20): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/agent/transactions?page=${page}&size=${size}`);
  }

  getTransactionStatus(id: number): Observable<TransactionResponse> {
    return this.http.get<TransactionResponse>(`${this.apiUrl}/agent/transactions/${id}/status`);
  }

  submitFeedback(id: number, request: AgentFeedbackRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/agent/transactions/${id}/feedback`, request);
  }

  submitBugReport(payload: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/ai/feedback/bug-report`, payload);
  }

  // --- Quick Checkout ---
  validateCheckout(request: QuickCheckoutValidateRequest): Observable<QuickCheckoutValidationResponse> {
    return this.http.post<QuickCheckoutValidationResponse>(`${this.apiUrl}/agent/quick-checkout/validate`, request);
  }

  executeCheckout(request: QuickCheckoutExecuteRequest): Observable<QuickCheckoutResponse> {
    return this.http.post<QuickCheckoutResponse>(`${this.apiUrl}/agent/quick-checkout/execute`, request);
  }

  // --- Coupons ---
  getApplicableCoupons(): Observable<ApplicableCoupon[]> {
    return this.http.get<ApplicableCoupon[]>(`${this.apiUrl}/agent/coupons/applicable`);
  }

  // --- AI Preferences ---
  getPreferences(): Observable<AiPreferenceResponse> {
    return this.http.get<AiPreferenceResponse>(`${this.apiUrl}/users/me/ai-preferences`);
  }

  updatePreferences(request: UpdateAiPreferenceRequest): Observable<AiPreferenceResponse> {
    return this.http.put<AiPreferenceResponse>(`${this.apiUrl}/users/me/ai-preferences`, request);
  }
}
