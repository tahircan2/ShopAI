export interface ApprovalResponse {
  id: number;
  approvalToken: string;
  planData: string;
  planHash: string;
  agentType: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
  expiresAt: string;
  respondedAt?: string;
  createdAt: string;
  remainingSeconds: number;
}

export interface ApprovalActionResponse {
  status: string;
  message: string;
  transactionId?: number;
}

export interface TransactionResponse {
  id: number;
  transactionType: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'AWAITING_APPROVAL' | 'COMPLETED' | 'FAILED' | 'ROLLED_BACK';
  totalSteps: number;
  completedSteps: number;
  failedStep?: number;
  totalDurationMs?: number;
  errorMessage?: string;
  resultOrderNumber?: string;
  feedbackScore?: number;
  createdAt: string;
  updatedAt: string;
  steps?: TransactionStepResponse[];
}

export interface TransactionStepResponse {
  id: number;
  stepOrder: number;
  stepType: string;
  status: string;
  stepDescription: string;
  errorMessage?: string;
  durationMs?: number;
  isRollbackable: boolean;
  createdAt: string;
}

export interface QuickCheckoutValidationResponse {
  valid: boolean;
  issues: ValidationIssue[];
  warnings: string[];
  cartSummary: CartSummary;
  applicableCoupons: ApplicableCoupon[];
}

export interface ValidationIssue {
  field: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}

export interface CartSummary {
  itemCount: number;
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  total: number;
  appliedCoupon?: string;
}

export interface ApplicableCoupon {
  code: string;
  description: string;
  discountType: string;
  discountValue: number;
  estimatedSaving: number;
}

export interface QuickCheckoutResponse {
  success: boolean;
  orderNumber?: string;
  transactionId?: number;
  message: string;
  totalAmount?: number;
}

export interface AiPreferenceResponse {
  autoApproveEnabled: boolean;
  autoApproveMaxAmount?: number;
  autoApproveCategories?: string[];
  useDefaultAddress: boolean;
  useDefaultPayment: boolean;
  dailyTransactionLimit: number;
  maxOrderAmount?: number;
  todayTransactionCount: number;
}
