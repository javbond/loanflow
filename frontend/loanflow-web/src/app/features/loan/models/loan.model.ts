export interface LoanApplication {
  id?: string;
  applicationNumber?: string;
  customerId: string;
  customerName?: string;
  loanType: LoanType;
  requestedAmount: number;
  approvedAmount?: number;
  interestRate?: number;
  tenureMonths: number;
  emiAmount?: number;
  status?: LoanStatus;
  workflowStage?: string;
  purpose: string;
  branchCode?: string;
  assignedOfficer?: string;
  cibilScore?: number;
  riskCategory?: string;
  processingFee?: number;
  expectedDisbursementDate?: string;
  submittedAt?: string;
  createdAt?: string;
  updatedAt?: string;
  documents?: DocumentInfo[];
  workflowHistory?: WorkflowHistory[];
  propertyDetails?: PropertyDetails;
  employmentDetails?: EmploymentDetails;
}

export interface LoanApplicationRequest {
  customerId: string;
  loanType: string;
  requestedAmount: number;
  tenureMonths: number;
  purpose: string;
  branchCode?: string;
  propertyDetails?: PropertyDetails;
  employmentDetails?: EmploymentDetails;
}

export interface PropertyDetails {
  propertyType: string;
  address: string;
  city: string;
  state: string;
  pinCode: string;
  estimatedValue?: number;
}

export interface EmploymentDetails {
  employmentType: EmploymentType;
  employerName?: string;
  businessName?: string;
  monthlyIncome: number;
  yearsOfExperience?: number;
}

export interface DocumentInfo {
  documentId: string;
  documentType: string;
  fileName: string;
  status: string;
  uploadedAt: string;
}

export interface WorkflowHistory {
  fromStage: string;
  toStage: string;
  action: string;
  performedBy: string;
  comments: string;
  timestamp: string;
}

export type LoanType =
  | 'HOME_LOAN'
  | 'PERSONAL_LOAN'
  | 'VEHICLE_LOAN'
  | 'BUSINESS_LOAN'
  | 'EDUCATION_LOAN'
  | 'GOLD_LOAN'
  | 'LAP';

export type LoanStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'DOCUMENT_VERIFICATION'
  | 'CREDIT_CHECK'
  | 'UNDERWRITING'
  | 'CONDITIONALLY_APPROVED'
  | 'REFERRED'
  | 'APPROVED'
  | 'DISBURSEMENT_PENDING'
  | 'DISBURSED'
  | 'RETURNED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'CLOSED'
  | 'NPA';

export type EmploymentType =
  | 'SALARIED'
  | 'SELF_EMPLOYED'
  | 'BUSINESS'
  | 'PROFESSIONAL';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// Constants for loan types with display info
export const LOAN_TYPES: { value: LoanType; label: string; baseRate: number; maxTenureYears: number }[] = [
  { value: 'HOME_LOAN', label: 'Home Loan', baseRate: 8.5, maxTenureYears: 30 },
  { value: 'PERSONAL_LOAN', label: 'Personal Loan', baseRate: 12.0, maxTenureYears: 7 },
  { value: 'VEHICLE_LOAN', label: 'Vehicle Loan', baseRate: 9.5, maxTenureYears: 7 },
  { value: 'BUSINESS_LOAN', label: 'Business Loan', baseRate: 14.0, maxTenureYears: 15 },
  { value: 'EDUCATION_LOAN', label: 'Education Loan', baseRate: 8.0, maxTenureYears: 15 },
  { value: 'GOLD_LOAN', label: 'Gold Loan', baseRate: 7.5, maxTenureYears: 3 },
  { value: 'LAP', label: 'Loan Against Property', baseRate: 9.0, maxTenureYears: 15 }
];

export const LOAN_STATUSES: { value: LoanStatus; label: string; color: string }[] = [
  { value: 'DRAFT', label: 'Draft', color: 'accent' },
  { value: 'SUBMITTED', label: 'Submitted', color: 'primary' },
  { value: 'DOCUMENT_VERIFICATION', label: 'Document Verification', color: 'primary' },
  { value: 'CREDIT_CHECK', label: 'Credit Check', color: 'primary' },
  { value: 'UNDERWRITING', label: 'Underwriting', color: 'primary' },
  { value: 'CONDITIONALLY_APPROVED', label: 'Conditionally Approved', color: 'accent' },
  { value: 'REFERRED', label: 'Referred to Senior', color: 'accent' },
  { value: 'APPROVED', label: 'Approved', color: 'primary' },
  { value: 'DISBURSEMENT_PENDING', label: 'Disbursement Pending', color: 'primary' },
  { value: 'DISBURSED', label: 'Disbursed', color: 'primary' },
  { value: 'RETURNED', label: 'Returned', color: 'warn' },
  { value: 'REJECTED', label: 'Rejected', color: 'warn' },
  { value: 'CANCELLED', label: 'Cancelled', color: 'warn' },
  { value: 'CLOSED', label: 'Closed', color: 'accent' },
  { value: 'NPA', label: 'NPA', color: 'warn' }
];

export const EMPLOYMENT_TYPES: { value: EmploymentType; label: string }[] = [
  { value: 'SALARIED', label: 'Salaried' },
  { value: 'SELF_EMPLOYED', label: 'Self Employed' },
  { value: 'BUSINESS', label: 'Business Owner' },
  { value: 'PROFESSIONAL', label: 'Professional' }
];

export const PROPERTY_TYPES = [
  { value: 'APARTMENT', label: 'Apartment/Flat' },
  { value: 'INDEPENDENT_HOUSE', label: 'Independent House' },
  { value: 'VILLA', label: 'Villa' },
  { value: 'PLOT', label: 'Plot/Land' },
  { value: 'COMMERCIAL', label: 'Commercial Property' },
  { value: 'INDUSTRIAL', label: 'Industrial Property' }
];
