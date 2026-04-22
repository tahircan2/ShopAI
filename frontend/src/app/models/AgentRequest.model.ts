export interface CreateApprovalRequest {
  planData: string;
  agentType: string;
  sessionId: string;
}

export interface QuickCheckoutValidateRequest {
  shippingAddressId?: number;
  couponCode?: string;
  paymentMethod?: string;
}

export interface QuickCheckoutExecuteRequest {
  approvalToken: string;
  shippingAddressId: number;
  couponCode?: string;
  paymentMethod: string;
  notes?: string;
}

export interface UpdateAiPreferenceRequest {
  autoApproveEnabled?: boolean;
  autoApproveMaxAmount?: number;
  autoApproveCategories?: string[];
  useDefaultAddress?: boolean;
  useDefaultPayment?: boolean;
}

export interface AgentFeedbackRequest {
  transactionId: number;
  score: number; // 1-5
  feedbackText?: string;
}
