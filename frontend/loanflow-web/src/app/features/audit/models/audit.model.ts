/**
 * Audit Trail models (US-030)
 */

export interface AuditEvent {
  id: string;
  eventType: string;
  description: string;
  serviceName: string;
  resourceType: string;
  resourceId: string;
  applicationId: string;
  customerId: string;
  performedBy: string;
  performedByName: string;
  performedByRole: string;
  beforeState: Record<string, any>;
  afterState: Record<string, any>;
  metadata: Record<string, any>;
  ipAddress: string;
  timestamp: string;
}

export interface AuditEventPage {
  content: AuditEvent[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// Event type display configuration
export const AUDIT_EVENT_CONFIG: Record<string, { label: string; icon: string; color: string }> = {
  APPLICATION_CREATED: { label: 'Application Created', icon: 'add_circle', color: '#4caf50' },
  APPLICATION_SUBMITTED: { label: 'Application Submitted', icon: 'send', color: '#2196f3' },
  APPLICATION_UPDATED: { label: 'Application Updated', icon: 'edit', color: '#ff9800' },
  STATUS_CHANGED: { label: 'Status Changed', icon: 'swap_horiz', color: '#9c27b0' },
  DECISION_MADE: { label: 'Decision Made', icon: 'gavel', color: '#607d8b' },
  APPROVAL_GRANTED: { label: 'Approved', icon: 'check_circle', color: '#4caf50' },
  APPLICATION_REJECTED: { label: 'Rejected', icon: 'cancel', color: '#f44336' },
  APPLICATION_REFERRED: { label: 'Referred', icon: 'forward', color: '#ff9800' },
  DOCUMENT_UPLOADED: { label: 'Document Uploaded', icon: 'upload_file', color: '#00bcd4' },
  DOCUMENT_VERIFIED: { label: 'Document Verified', icon: 'verified', color: '#4caf50' },
  DOCUMENT_REJECTED: { label: 'Document Rejected', icon: 'block', color: '#f44336' },
  KYC_INITIATED: { label: 'KYC Initiated', icon: 'fingerprint', color: '#2196f3' },
  KYC_VERIFIED: { label: 'KYC Verified', icon: 'verified_user', color: '#4caf50' },
  TASK_ASSIGNED: { label: 'Task Assigned', icon: 'assignment_ind', color: '#3f51b5' },
  TASK_COMPLETED: { label: 'Task Completed', icon: 'task_alt', color: '#4caf50' },
  CREDIT_CHECK_COMPLETED: { label: 'Credit Check', icon: 'credit_score', color: '#795548' },
  INCOME_VERIFIED: { label: 'Income Verified', icon: 'account_balance', color: '#009688' },
  CUSTOMER_CREATED: { label: 'Customer Created', icon: 'person_add', color: '#4caf50' },
  CUSTOMER_UPDATED: { label: 'Customer Updated', icon: 'person', color: '#ff9800' },
};

export const AUDIT_EVENT_TYPES = Object.keys(AUDIT_EVENT_CONFIG);
