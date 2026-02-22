/**
 * Credit Bureau Integration — TypeScript interfaces
 * Maps to backend DTOs in com.loanflow.loan.creditbureau.dto
 */

export type BureauDataSource = 'REAL' | 'CACHED' | 'SIMULATED';

export interface CreditBureauResponse {
  pan: string;
  creditScore: number;
  scoreVersion: string;
  scoreFactors: string[];
  accounts: AccountSummary[];
  enquiries: EnquirySummary[];
  dpd90PlusCount: number;
  writtenOffAccounts: number;
  enquiryCount30Days: number;
  totalActiveAccounts: number;
  totalOutstandingBalance: number;
  dataSource: BureauDataSource;
  pullTimestamp: string;
  controlNumber: string;
}

export interface AccountSummary {
  accountType: string;
  lenderName: string;
  currentBalance: number;
  amountOverdue: number;
  dpdStatus: string;
  accountStatus: string;
  openDate: string;
  lastPaymentDate: string;
}

export interface EnquirySummary {
  enquiryDate: string;
  memberName: string;
  purpose: string;
  amount: number;
}

export interface CreditPullRequest {
  applicationId?: string;
  pan?: string;
}

export const BUREAU_SOURCE_COLORS: Record<BureauDataSource, string> = {
  REAL: 'primary',
  CACHED: 'accent',
  SIMULATED: 'warn'
};

export const BUREAU_SOURCE_LABELS: Record<BureauDataSource, string> = {
  REAL: 'Live Bureau Pull',
  CACHED: 'Cached Report',
  SIMULATED: 'Simulated Data'
};
