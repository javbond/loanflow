// Document Response from API
export interface Document {
  id: string;
  documentNumber: string;
  applicationId: string;
  customerId?: string;
  documentType: string;
  category: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  formattedFileSize: string;
  status: string;
  storageBucket: string;
  storageKey: string;
  downloadUrl?: string;
  uploadedAt: string;
  uploadedBy?: string;
  verifiedAt?: string;
  verifiedBy?: string;
  verificationRemarks?: string;
  aadhaarVerified?: boolean;
  panVerified?: boolean;
  expiryDate?: string;
  expired?: boolean;
  expiringSoon?: boolean;
  version: number;
  previousVersionId?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// Request DTOs
export interface DocumentUploadRequest {
  applicationId: string;
  customerId?: string;
  documentType: string;
  description?: string;
  expiryDate?: string;
  password?: string;
}

export interface DocumentVerificationRequest {
  verifierId: string;
  approved: boolean;
  remarks?: string;
}

// Document Type Definition
export interface DocumentTypeInfo {
  value: string;
  label: string;
  category: string;
  expiryMonths?: number;
}

// Document Status Definition
export interface DocumentStatusInfo {
  value: string;
  label: string;
  color: string;
  icon: string;
}

// Document Category Definition
export interface DocumentCategoryInfo {
  value: string;
  label: string;
  mandatory: boolean;
}

// ============ CONSTANTS ============

// Document Types - 26 types grouped by category
export const DOCUMENT_TYPES: DocumentTypeInfo[] = [
  // KYC Documents
  { value: 'PAN_CARD', label: 'PAN Card', category: 'KYC' },
  { value: 'AADHAAR_CARD', label: 'Aadhaar Card', category: 'KYC' },
  { value: 'PASSPORT', label: 'Passport', category: 'KYC', expiryMonths: 60 },
  { value: 'VOTER_ID', label: 'Voter ID', category: 'KYC' },
  { value: 'DRIVING_LICENSE', label: 'Driving License', category: 'KYC', expiryMonths: 60 },

  // Income Documents
  { value: 'SALARY_SLIP', label: 'Salary Slip', category: 'INCOME', expiryMonths: 3 },
  { value: 'FORM_16', label: 'Form 16', category: 'INCOME', expiryMonths: 12 },
  { value: 'ITR', label: 'Income Tax Return (ITR)', category: 'INCOME', expiryMonths: 12 },
  { value: 'EMPLOYMENT_LETTER', label: 'Employment Letter', category: 'INCOME', expiryMonths: 3 },

  // Financial Documents
  { value: 'BANK_STATEMENT', label: 'Bank Statement', category: 'FINANCIAL', expiryMonths: 6 },
  { value: 'EXISTING_LOAN_STATEMENT', label: 'Existing Loan Statement', category: 'FINANCIAL', expiryMonths: 3 },
  { value: 'CREDIT_CARD_STATEMENT', label: 'Credit Card Statement', category: 'FINANCIAL', expiryMonths: 6 },

  // Property Documents
  { value: 'PROPERTY_DEED', label: 'Property Deed', category: 'PROPERTY' },
  { value: 'SALE_AGREEMENT', label: 'Sale Agreement', category: 'PROPERTY' },
  { value: 'PROPERTY_TAX_RECEIPT', label: 'Property Tax Receipt', category: 'PROPERTY', expiryMonths: 12 },
  { value: 'ENCUMBRANCE_CERTIFICATE', label: 'Encumbrance Certificate', category: 'PROPERTY', expiryMonths: 12 },
  { value: 'APPROVED_PLAN', label: 'Approved Building Plan', category: 'PROPERTY' },
  { value: 'NOC', label: 'No Objection Certificate (NOC)', category: 'PROPERTY' },

  // Vehicle Documents
  { value: 'VEHICLE_RC', label: 'Vehicle RC', category: 'VEHICLE' },
  { value: 'VEHICLE_INSURANCE', label: 'Vehicle Insurance', category: 'VEHICLE', expiryMonths: 12 },
  { value: 'QUOTATION', label: 'Vehicle Quotation', category: 'VEHICLE', expiryMonths: 3 },

  // Business Documents
  { value: 'GST_REGISTRATION', label: 'GST Registration', category: 'BUSINESS' },
  { value: 'BUSINESS_LICENSE', label: 'Business License', category: 'BUSINESS', expiryMonths: 60 },
  { value: 'PARTNERSHIP_DEED', label: 'Partnership Deed', category: 'BUSINESS' },
  { value: 'MOA_AOA', label: 'MOA/AOA', category: 'BUSINESS' },
  { value: 'BALANCE_SHEET', label: 'Balance Sheet', category: 'BUSINESS', expiryMonths: 12 },
  { value: 'PROFIT_LOSS', label: 'Profit & Loss Statement', category: 'BUSINESS', expiryMonths: 12 },

  // Other Documents
  { value: 'PHOTOGRAPH', label: 'Photograph', category: 'OTHER' },
  { value: 'SIGNATURE', label: 'Signature', category: 'OTHER' },
  { value: 'CANCELLED_CHEQUE', label: 'Cancelled Cheque', category: 'OTHER' },
  { value: 'OTHER', label: 'Other Document', category: 'OTHER' }
];

// Document Statuses - 8 states
export const DOCUMENT_STATUSES: DocumentStatusInfo[] = [
  { value: 'PENDING', label: 'Pending Upload', color: 'warn', icon: 'hourglass_empty' },
  { value: 'UPLOADED', label: 'Uploaded', color: 'primary', icon: 'cloud_done' },
  { value: 'UPLOAD_FAILED', label: 'Upload Failed', color: 'warn', icon: 'cloud_off' },
  { value: 'VERIFIED', label: 'Verified', color: 'accent', icon: 'verified' },
  { value: 'REJECTED', label: 'Rejected', color: 'warn', icon: 'cancel' },
  { value: 'EXPIRED', label: 'Expired', color: 'warn', icon: 'event_busy' },
  { value: 'DELETED', label: 'Deleted', color: 'warn', icon: 'delete' },
  { value: 'ARCHIVED', label: 'Archived', color: 'primary', icon: 'archive' }
];

// Document Categories - 7 categories
export const DOCUMENT_CATEGORIES: DocumentCategoryInfo[] = [
  { value: 'KYC', label: 'KYC Documents', mandatory: true },
  { value: 'INCOME', label: 'Income Proof', mandatory: true },
  { value: 'FINANCIAL', label: 'Financial Documents', mandatory: true },
  { value: 'PROPERTY', label: 'Property Documents', mandatory: false },
  { value: 'VEHICLE', label: 'Vehicle Documents', mandatory: false },
  { value: 'BUSINESS', label: 'Business Documents', mandatory: false },
  { value: 'OTHER', label: 'Other Documents', mandatory: false }
];

// File Upload Constraints
export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB in bytes
export const MAX_FILE_SIZE_DISPLAY = '10 MB';

export const ALLOWED_FILE_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/tiff',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
];

export const ALLOWED_FILE_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png', '.tiff', '.doc', '.docx'];

// Helper Functions
export function getDocumentTypeInfo(type: string): DocumentTypeInfo | undefined {
  return DOCUMENT_TYPES.find(t => t.value === type);
}

export function getDocumentStatusInfo(status: string): DocumentStatusInfo | undefined {
  return DOCUMENT_STATUSES.find(s => s.value === status);
}

export function getDocumentCategoryInfo(category: string): DocumentCategoryInfo | undefined {
  return DOCUMENT_CATEGORIES.find(c => c.value === category);
}

export function getDocumentTypesByCategory(category: string): DocumentTypeInfo[] {
  return DOCUMENT_TYPES.filter(t => t.category === category);
}

export function isFileTypeAllowed(mimeType: string): boolean {
  return ALLOWED_FILE_TYPES.includes(mimeType);
}

export function isFileSizeAllowed(sizeInBytes: number): boolean {
  return sizeInBytes <= MAX_FILE_SIZE;
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export function getStatusColor(status: string): string {
  const statusInfo = getDocumentStatusInfo(status);
  return statusInfo?.color || 'primary';
}

export function isDocumentVerifiable(status: string): boolean {
  return status === 'UPLOADED' || status === 'PENDING';
}

export function requiresReupload(status: string): boolean {
  return status === 'REJECTED' || status === 'UPLOAD_FAILED' || status === 'EXPIRED';
}
