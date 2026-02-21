// ==================== Policy Enums ====================

export type PolicyCategory = 'ELIGIBILITY' | 'PRICING' | 'CREDIT_LIMIT' | 'DOCUMENT_REQUIREMENT' | 'WORKFLOW' | 'RISK_SCORING';
export type PolicyStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
export type LoanType = 'PERSONAL_LOAN' | 'HOME_LOAN' | 'VEHICLE_LOAN' | 'EDUCATION_LOAN' | 'GOLD_LOAN' | 'BUSINESS_LOAN' | 'KCC' | 'LAP' | 'ALL';
export type ConditionOperator = 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN' | 'LESS_THAN_OR_EQUAL' | 'IN' | 'NOT_IN' | 'BETWEEN' | 'CONTAINS' | 'STARTS_WITH' | 'IS_TRUE' | 'IS_FALSE' | 'IS_NULL' | 'IS_NOT_NULL';
export type ActionType = 'APPROVE' | 'REJECT' | 'REFER' | 'SET_INTEREST_RATE' | 'SET_MAX_AMOUNT' | 'REQUIRE_DOCUMENT' | 'ADD_FLAG' | 'SET_PROCESSING_FEE' | 'ESCALATE' | 'NOTIFY';
export type LogicalOperator = 'AND' | 'OR';

// ==================== Response DTOs ====================

export interface PolicyResponse {
  id: string;
  policyCode: string;
  name: string;
  description?: string;
  category: PolicyCategory;
  loanType: LoanType;
  status: PolicyStatus;
  versionNumber: number;
  previousVersionId?: string;
  rules: PolicyRuleResponse[];
  ruleCount: number;
  priority: number;
  effectiveFrom?: string;
  effectiveUntil?: string;
  tags: string[];
  createdBy?: string;
  modifiedBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PolicyRuleResponse {
  name: string;
  description?: string;
  logicalOperator: LogicalOperator;
  conditions: ConditionResponse[];
  actions: ActionResponse[];
  priority: number;
  enabled: boolean;
}

export interface ConditionResponse {
  field: string;
  operator: ConditionOperator;
  value?: string;
  values?: string[];
  minValue?: string;
  maxValue?: string;
}

export interface ActionResponse {
  type: ActionType;
  parameters: { [key: string]: string };
  description?: string;
}

// ==================== Request DTOs ====================

export interface PolicyRequest {
  name: string;
  description?: string;
  category: string;
  loanType: string;
  priority?: number;
  effectiveFrom?: string;
  effectiveUntil?: string;
  tags?: string[];
  rules?: PolicyRuleRequest[];
}

export interface PolicyRuleRequest {
  name: string;
  description?: string;
  logicalOperator: string;
  conditions: ConditionRequest[];
  actions: ActionRequest[];
  priority?: number;
  enabled?: boolean;
}

export interface ConditionRequest {
  field: string;
  operator: string;
  value?: string;
  values?: string[];
  minValue?: string;
  maxValue?: string;
}

export interface ActionRequest {
  type: string;
  parameters?: { [key: string]: string };
  description?: string;
}

// ==================== Shared Response Wrappers ====================

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ==================== Stats ====================

export interface PolicyStatsResponse {
  totalPolicies: number;
  activePolicies: number;
  draftPolicies: number;
  inactivePolicies: number;
  archivedPolicies: number;
}

// ==================== Constants ====================

export const POLICY_CATEGORIES: { value: PolicyCategory; label: string }[] = [
  { value: 'ELIGIBILITY', label: 'Eligibility' },
  { value: 'PRICING', label: 'Pricing' },
  { value: 'CREDIT_LIMIT', label: 'Credit Limit' },
  { value: 'DOCUMENT_REQUIREMENT', label: 'Document Requirement' },
  { value: 'WORKFLOW', label: 'Workflow' },
  { value: 'RISK_SCORING', label: 'Risk Scoring' }
];

export const LOAN_TYPES: { value: LoanType; label: string }[] = [
  { value: 'PERSONAL_LOAN', label: 'Personal Loan' },
  { value: 'HOME_LOAN', label: 'Home Loan' },
  { value: 'VEHICLE_LOAN', label: 'Vehicle Loan' },
  { value: 'EDUCATION_LOAN', label: 'Education Loan' },
  { value: 'GOLD_LOAN', label: 'Gold Loan' },
  { value: 'BUSINESS_LOAN', label: 'Business Loan' },
  { value: 'KCC', label: 'Kisan Credit Card' },
  { value: 'LAP', label: 'Loan Against Property' },
  { value: 'ALL', label: 'All Loan Types' }
];

export const CONDITION_OPERATORS: { value: ConditionOperator; label: string }[] = [
  { value: 'EQUALS', label: 'Equals' },
  { value: 'NOT_EQUALS', label: 'Not Equals' },
  { value: 'GREATER_THAN', label: 'Greater Than' },
  { value: 'GREATER_THAN_OR_EQUAL', label: 'Greater Than or Equal' },
  { value: 'LESS_THAN', label: 'Less Than' },
  { value: 'LESS_THAN_OR_EQUAL', label: 'Less Than or Equal' },
  { value: 'IN', label: 'In List' },
  { value: 'NOT_IN', label: 'Not In List' },
  { value: 'BETWEEN', label: 'Between' },
  { value: 'CONTAINS', label: 'Contains' },
  { value: 'STARTS_WITH', label: 'Starts With' },
  { value: 'IS_TRUE', label: 'Is True' },
  { value: 'IS_FALSE', label: 'Is False' },
  { value: 'IS_NULL', label: 'Is Null' },
  { value: 'IS_NOT_NULL', label: 'Is Not Null' }
];

export const ACTION_TYPES: { value: ActionType; label: string }[] = [
  { value: 'APPROVE', label: 'Approve' },
  { value: 'REJECT', label: 'Reject' },
  { value: 'REFER', label: 'Refer for Review' },
  { value: 'SET_INTEREST_RATE', label: 'Set Interest Rate' },
  { value: 'SET_MAX_AMOUNT', label: 'Set Max Amount' },
  { value: 'REQUIRE_DOCUMENT', label: 'Require Document' },
  { value: 'ADD_FLAG', label: 'Add Flag' },
  { value: 'SET_PROCESSING_FEE', label: 'Set Processing Fee' },
  { value: 'ESCALATE', label: 'Escalate' },
  { value: 'NOTIFY', label: 'Send Notification' }
];
