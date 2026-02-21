import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RiskDashboardResponse,
  ScoreDistribution,
  RiskTierBreakdown,
  NegativeMarkerAlert,
  SummaryKpis
} from '../models/risk-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class RiskDashboardService {
  private apiUrl = '/api/v1/risk/dashboard';

  constructor(private http: HttpClient) {}

  /**
   * Get the full risk dashboard data.
   */
  getDashboard(): Observable<RiskDashboardResponse> {
    return this.http.get<RiskDashboardResponse>(this.apiUrl);
  }

  /**
   * Get only the CIBIL score distribution.
   */
  getScoreDistribution(): Observable<ScoreDistribution[]> {
    return this.http.get<ScoreDistribution[]>(`${this.apiUrl}/score-distribution`);
  }

  /**
   * Get only the risk tier breakdown.
   */
  getRiskTiers(): Observable<RiskTierBreakdown[]> {
    return this.http.get<RiskTierBreakdown[]>(`${this.apiUrl}/risk-tiers`);
  }

  /**
   * Get only the negative marker alerts.
   */
  getNegativeMarkers(): Observable<NegativeMarkerAlert[]> {
    return this.http.get<NegativeMarkerAlert[]>(`${this.apiUrl}/negative-markers`);
  }

  /**
   * Get only the summary KPIs.
   */
  getSummary(): Observable<SummaryKpis> {
    return this.http.get<SummaryKpis>(`${this.apiUrl}/summary`);
  }
}
