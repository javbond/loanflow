import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { RiskDashboardService } from '../../services/risk-dashboard.service';
import {
  RiskDashboardResponse,
  NegativeMarkerAlert
} from '../../models/risk-dashboard.model';

@Component({
  selector: 'app-risk-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTableModule,
    MatBadgeModule,
    MatTooltipModule,
    RouterModule
  ],
  templateUrl: './risk-dashboard.component.html',
  styleUrl: './risk-dashboard.component.css'
})
export class RiskDashboardComponent implements OnInit {
  dashboard: RiskDashboardResponse | null = null;
  loading = true;
  error: string | null = null;

  // For negative markers table
  displayedColumns = ['severity', 'applicationNumber', 'loanType', 'amount', 'cibilScore', 'status', 'alertType'];

  constructor(private dashboardService: RiskDashboardService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.dashboardService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load risk dashboard data. Please try again.';
        this.loading = false;
        console.error('Risk dashboard error:', err);
      }
    });
  }

  refresh(): void {
    this.loadDashboard();
  }

  // Helper methods for the template

  getMaxScoreCount(): number {
    if (!this.dashboard?.scoreDistribution) return 1;
    return Math.max(...this.dashboard.scoreDistribution.map(d => d.count), 1);
  }

  getBarWidth(count: number): number {
    return (count / this.getMaxScoreCount()) * 100;
  }

  getSeverityIcon(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return 'error';
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      default: return 'help_outline';
    }
  }

  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return '#ef4444';
      case 'HIGH': return '#f97316';
      case 'MEDIUM': return '#eab308';
      default: return '#6b7280';
    }
  }

  getAlertTypeLabel(alertType: string): string {
    switch (alertType) {
      case 'NPA': return 'Non-Performing Asset';
      case 'LOW_CREDIT_SCORE': return 'Low Credit Score';
      case 'HIGH_RISK_REJECTION': return 'High Risk Rejection';
      case 'RISK_FLAG': return 'Risk Flag';
      default: return alertType;
    }
  }

  formatAmount(amount: number): string {
    if (amount >= 10000000) {
      return '\u20B9' + (amount / 10000000).toFixed(2) + ' Cr';
    } else if (amount >= 100000) {
      return '\u20B9' + (amount / 100000).toFixed(2) + ' L';
    } else {
      return '\u20B9' + amount.toLocaleString('en-IN');
    }
  }

  getExposureMaxAmount(): number {
    if (!this.dashboard?.portfolioExposure) return 1;
    return Math.max(...this.dashboard.portfolioExposure.map(e => e.totalAmount), 1);
  }

  getExposureBarWidth(amount: number): number {
    return (amount / this.getExposureMaxAmount()) * 100;
  }

  getRiskCategoryColor(category: string): string {
    switch (category) {
      case 'LOW': return '#22c55e';
      case 'MEDIUM': return '#3b82f6';
      case 'MEDIUM_HIGH': return '#f97316';
      case 'HIGH': return '#ef4444';
      default: return '#6b7280';
    }
  }
}
