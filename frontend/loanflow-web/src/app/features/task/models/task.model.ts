export interface TaskResponse {
  taskId: string;
  taskName: string;
  taskDefinitionKey: string;
  assignee: string | null;
  candidateGroups: string[];
  applicationId: string;
  applicationNumber: string;
  loanType: string;
  customerEmail: string;
  requestedAmount: string;
  cibilScore: number | null;
  riskCategory: string | null;
  processInstanceId: string;
  createdAt: string;
  dueDate: string | null;
  formKey: string | null;
}

export interface CompleteTaskRequest {
  decision: TaskDecision;
  comments?: string;
  approvedAmount?: number;
  interestRate?: number;
  rejectionReason?: string;
}

export type TaskDecision = 'APPROVED' | 'REJECTED' | 'REFERRED';

// Spring Page<T> response format (used by inbox/my-tasks endpoints)
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Reuse ApiResponse from loan model for single-item endpoints
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export const TASK_DEFINITIONS: { key: string; label: string; icon: string }[] = [
  { key: 'documentVerification', label: 'Document Verification', icon: 'description' },
  { key: 'underwritingReview', label: 'Underwriting Review', icon: 'rate_review' },
  { key: 'referredReview', label: 'Senior Review', icon: 'supervisor_account' }
];

export const RISK_COLORS: Record<string, string> = {
  LOW: 'primary',
  MEDIUM: 'accent',
  HIGH: 'warn'
};

export function getTaskLabel(definitionKey: string): string {
  const def = TASK_DEFINITIONS.find(d => d.key === definitionKey);
  return def?.label ?? definitionKey;
}

export function formatLoanType(loanType: string): string {
  return loanType?.replace(/_/g, ' ') ?? '';
}

export function formatCurrency(amount: string | number): string {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) return '-';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(num);
}
