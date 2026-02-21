/**
 * Risk Dashboard TypeScript models — matches backend RiskDashboardResponse.
 */

export interface RiskDashboardResponse {
  scoreDistribution: ScoreDistribution[];
  riskTierBreakdown: RiskTierBreakdown[];
  negativeMarkerAlerts: NegativeMarkerAlert[];
  summaryKpis: SummaryKpis;
  loanTypeRiskBreakdown: LoanTypeRiskBreakdown[];
  portfolioExposure: PortfolioExposure[];
  generatedAt: string;
}

export interface ScoreDistribution {
  range: string;
  label: string;
  count: number;
  color: string;
}

export interface RiskTierBreakdown {
  category: string;
  label: string;
  count: number;
  percentage: number;
  color: string;
}

export interface NegativeMarkerAlert {
  applicationId: string;
  applicationNumber: string;
  loanType: string;
  requestedAmount: number;
  cibilScore: number | null;
  riskCategory: string;
  status: string;
  alertType: string;
  severity: string;
  reason: string | null;
  lastUpdated: string;
}

export interface SummaryKpis {
  averageCibilScore: number;
  highRiskCount: number;
  totalProcessed: number;
  npaCount: number;
  rejectedCount: number;
  rejectionRate: number;
}

export interface LoanTypeRiskBreakdown {
  loanType: string;
  riskCounts: { [key: string]: number };
}

export interface PortfolioExposure {
  riskCategory: string;
  totalAmount: number;
  percentage: number;
}
