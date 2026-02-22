/**
 * Income Verification Integration — TypeScript interfaces
 * Maps to backend DTOs in com.loanflow.loan.incomeverification.dto
 */

export type IncomeDataSource = 'REAL' | 'CACHED' | 'SIMULATED';

export interface IncomeVerificationResponse {
  pan: string;
  incomeVerified: boolean;
  verifiedMonthlyIncome: number;
  dtiRatio: number;
  incomeConsistencyScore: number;
  itrData: ItrData | null;
  gstData: GstData | null;
  bankStatementData: BankStatementData | null;
  flags: string[];
  dataSource: IncomeDataSource;
  verificationTimestamp: string;
}

export interface ItrData {
  grossTotalIncome: number;
  salaryIncome: number;
  businessIncome: number;
  itrFormType: string;
  assessmentYear: string;
  filedOnTime: boolean;
}

export interface GstData {
  gstin: string;
  annualTurnover: number;
  complianceRating: string;
  filingCount: number;
  active: boolean;
}

export interface BankStatementData {
  avgMonthlyBalance: number;
  avgMonthlyCredits: number;
  bounceCount: number;
  monthlyBalances: number[];
  monthsAnalyzed: number;
  minBalance: number;
  maxBalance: number;
}

export interface IncomeVerificationRequest {
  pan?: string;
  gstin?: string;
  employmentType?: string;
  declaredMonthlyIncome?: number;
}

export const INCOME_SOURCE_COLORS: Record<IncomeDataSource, string> = {
  REAL: 'primary',
  CACHED: 'accent',
  SIMULATED: 'warn'
};

export const INCOME_SOURCE_LABELS: Record<IncomeDataSource, string> = {
  REAL: 'Live Verification',
  CACHED: 'Cached Result',
  SIMULATED: 'Simulated Data'
};

export const GST_COMPLIANCE_COLORS: Record<string, string> = {
  EXCELLENT: 'primary',
  GOOD: 'primary',
  FAIR: 'accent',
  POOR: 'warn'
};
