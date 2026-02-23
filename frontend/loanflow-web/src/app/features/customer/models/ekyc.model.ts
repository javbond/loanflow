/**
 * e-KYC model interfaces (US-029)
 */

export interface EkycData {
  name: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  photo?: string;
  pinCode?: string;
  state?: string;
  district?: string;
}

export interface EkycInitiateRequest {
  aadhaarNumber: string;
}

export interface EkycVerifyRequest {
  transactionId: string;
  otp: string;
}

export interface EkycInitiateResponse {
  transactionId: string;
  maskedMobile: string;
  status: string;
  message: string;
}

export interface EkycVerifyResponse {
  verified: boolean;
  transactionId: string;
  status: string;
  message: string;
  ekycData?: EkycData;
  ckycNumber?: string;
}

export interface KycStatusResponse {
  customerId: string;
  status: string;
  verifiedAt?: string;
  ckycNumber?: string;
  ekycData?: EkycData;
  attemptCount: number;
  maskedAadhaar?: string;
  message: string;
}

export type EkycPanelState = 'IDLE' | 'INITIATING' | 'OTP_INPUT' | 'VERIFYING' | 'VERIFIED' | 'FAILED';

export const KYC_STATUS_COLORS: Record<string, string> = {
  'VERIFIED': 'primary',
  'OTP_SENT': 'accent',
  'PENDING': '',
  'FAILED': 'warn',
  'EXPIRED': 'warn',
  'NOT_INITIATED': '',
  'ALREADY_VERIFIED': 'primary',
  'MAX_ATTEMPTS_EXCEEDED': 'warn'
};

export const KYC_STATUS_LABELS: Record<string, string> = {
  'VERIFIED': 'Verified',
  'OTP_SENT': 'OTP Sent',
  'PENDING': 'Pending',
  'FAILED': 'Failed',
  'EXPIRED': 'Expired',
  'NOT_INITIATED': 'Not Initiated',
  'ALREADY_VERIFIED': 'Already Verified',
  'MAX_ATTEMPTS_EXCEEDED': 'Max Attempts Exceeded'
};
