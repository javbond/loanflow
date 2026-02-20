import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication, LOAN_TYPES, LOAN_STATUSES } from '../../../loan/models/loan.model';

/**
 * My Applications Component
 * Lists customer's loan applications with status
 * Issue: #28 [US-026] Loan Offer Accept/Reject
 */
@Component({
  selector: 'app-my-applications',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatPaginatorModule
  ],
  template: `
    <div class="applications-container">
      <div class="header-section">
        <div class="title-section">
          <h1><mat-icon>assignment</mat-icon> My Loan Applications</h1>
          <p class="subtitle">Track the status of your loan applications</p>
        </div>
        <button mat-raised-button color="primary" routerLink="/my-portal/apply">
          <mat-icon>add_circle</mat-icon> Apply for New Loan
        </button>
      </div>

      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading your applications...</p>
        </div>
      } @else if (applications.length === 0) {
        <mat-card class="empty-state-card">
          <mat-card-content>
            <div class="empty-state">
              <mat-icon class="empty-icon">inbox</mat-icon>
              <h2>No Applications Yet</h2>
              <p>You haven't submitted any loan applications. Start your journey by applying for a loan.</p>
              <button mat-raised-button color="primary" routerLink="/my-portal/apply">
                <mat-icon>add_circle</mat-icon> Apply for a Loan
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      } @else {
        <div class="applications-list">
          @for (app of applications; track app.id) {
            <mat-card class="application-card" [class]="getStatusClass(app.status)">
              <mat-card-header>
                <mat-icon mat-card-avatar class="loan-icon">{{ getLoanTypeIcon(app.loanType) }}</mat-icon>
                <mat-card-title>{{ getLoanTypeLabel(app.loanType) }}</mat-card-title>
                <mat-card-subtitle>{{ app.applicationNumber }}</mat-card-subtitle>
              </mat-card-header>

              <mat-card-content>
                <div class="application-details">
                  <div class="detail-row">
                    <span class="label">Requested Amount</span>
                    <span class="value amount">{{ formatCurrency(app.requestedAmount) }}</span>
                  </div>
                  @if (app.approvedAmount) {
                    <div class="detail-row">
                      <span class="label">Approved Amount</span>
                      <span class="value amount approved">{{ formatCurrency(app.approvedAmount) }}</span>
                    </div>
                  }
                  <div class="detail-row">
                    <span class="label">Tenure</span>
                    <span class="value">{{ app.tenureMonths }} months</span>
                  </div>
                  @if (app.interestRate) {
                    <div class="detail-row">
                      <span class="label">Interest Rate</span>
                      <span class="value">{{ app.interestRate }}% p.a.</span>
                    </div>
                  }
                  @if (app.emiAmount) {
                    <div class="detail-row">
                      <span class="label">EMI</span>
                      <span class="value emi">{{ formatCurrency(app.emiAmount) }}/month</span>
                    </div>
                  }
                  <mat-divider></mat-divider>
                  <div class="detail-row status-row">
                    <span class="label">Status</span>
                    <mat-chip [class]="'status-chip ' + app.status?.toLowerCase()">
                      {{ getStatusLabel(app.status) }}
                    </mat-chip>
                  </div>
                  <div class="detail-row">
                    <span class="label">Applied On</span>
                    <span class="value">{{ formatDate(app.createdAt) }}</span>
                  </div>
                </div>
              </mat-card-content>

              <mat-card-actions>
                <button mat-button color="primary" [routerLink]="['/my-portal/applications', app.id]">
                  <mat-icon>visibility</mat-icon> View Details
                </button>
                @if (app.status === 'APPROVED') {
                  <button mat-raised-button color="accent" [routerLink]="['/my-portal/applications', app.id]">
                    <mat-icon>check_circle</mat-icon> View Offer
                  </button>
                }
                @if (app.status === 'SUBMITTED' || app.status === 'DOCUMENT_VERIFICATION') {
                  <button mat-button color="primary" [routerLink]="['/my-portal/documents/upload', app.id]">
                    <mat-icon>upload_file</mat-icon> Upload Documents
                  </button>
                }
              </mat-card-actions>
            </mat-card>
          }
        </div>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageIndex]="pageIndex"
          [pageSizeOptions]="[5, 10, 20]"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .applications-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .header-section {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }

    .title-section h1 {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0;
      font-size: 1.75rem;
      color: #1976d2;
    }

    .subtitle {
      color: #666;
      margin: 8px 0 0;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #666;
    }

    .loading-container p {
      margin-top: 16px;
    }

    .empty-state-card {
      max-width: 500px;
      margin: 48px auto;
    }

    .empty-state {
      text-align: center;
      padding: 32px;
    }

    .empty-icon {
      font-size: 72px;
      width: 72px;
      height: 72px;
      color: #ccc;
    }

    .empty-state h2 {
      margin: 16px 0 8px;
      color: #333;
    }

    .empty-state p {
      color: #666;
      margin-bottom: 24px;
    }

    .applications-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
      gap: 24px;
      margin-bottom: 24px;
    }

    .application-card {
      border-left: 4px solid #1976d2;
      transition: box-shadow 0.3s ease;
    }

    .application-card:hover {
      box-shadow: 0 6px 16px rgba(0,0,0,0.15);
    }

    .application-card.approved {
      border-left-color: #4caf50;
      background: linear-gradient(135deg, #ffffff 0%, #e8f5e9 100%);
    }

    .application-card.rejected, .application-card.cancelled {
      border-left-color: #f44336;
    }

    .application-card.disbursed, .application-card.disbursement_pending {
      border-left-color: #2196f3;
    }

    .loan-icon {
      background: linear-gradient(135deg, #1976d2, #42a5f5);
      color: white;
      border-radius: 50%;
      padding: 8px;
      font-size: 28px;
      width: 44px;
      height: 44px;
    }

    .application-details {
      padding: 8px 0;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
    }

    .detail-row .label {
      color: #666;
      font-size: 0.875rem;
    }

    .detail-row .value {
      font-weight: 500;
      color: #333;
    }

    .detail-row .value.amount {
      font-size: 1.1rem;
      color: #1976d2;
    }

    .detail-row .value.approved {
      color: #4caf50;
    }

    .detail-row .value.emi {
      color: #ff9800;
    }

    .status-row {
      margin-top: 8px;
    }

    .status-chip {
      font-weight: 500;
    }

    .status-chip.submitted { background: #e3f2fd; color: #1565c0; }
    .status-chip.document_verification { background: #fff3e0; color: #e65100; }
    .status-chip.credit_check { background: #f3e5f5; color: #7b1fa2; }
    .status-chip.underwriting { background: #e8eaf6; color: #3949ab; }
    .status-chip.approved { background: #e8f5e9; color: #2e7d32; }
    .status-chip.disbursement_pending { background: #e1f5fe; color: #0277bd; }
    .status-chip.disbursed { background: #e0f2f1; color: #00695c; }
    .status-chip.rejected { background: #ffebee; color: #c62828; }
    .status-chip.cancelled { background: #fafafa; color: #616161; }
    .status-chip.returned { background: #fff8e1; color: #f57f17; }

    mat-card-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      padding: 8px 16px 16px !important;
    }

    mat-paginator {
      margin-top: 16px;
    }

    @media (max-width: 600px) {
      .applications-list {
        grid-template-columns: 1fr;
      }

      .header-section {
        flex-direction: column;
      }
    }
  `]
})
export class MyApplicationsComponent implements OnInit {
  private loanService = inject(LoanService);

  applications: LoanApplication[] = [];
  loading = true;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 10;

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.loading = true;
    this.loanService.getMyApplications(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.applications = response.content || [];
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.applications = [];
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadApplications();
  }

  getLoanTypeLabel(type: string): string {
    return LOAN_TYPES.find(t => t.value === type)?.label || type;
  }

  getLoanTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      HOME_LOAN: 'home',
      PERSONAL_LOAN: 'person',
      VEHICLE_LOAN: 'directions_car',
      BUSINESS_LOAN: 'business',
      EDUCATION_LOAN: 'school',
      GOLD_LOAN: 'monetization_on',
      LAP: 'apartment'
    };
    return icons[type] || 'account_balance';
  }

  getStatusLabel(status?: string): string {
    return LOAN_STATUSES.find(s => s.value === status)?.label || status || 'Unknown';
  }

  getStatusClass(status?: string): string {
    return status?.toLowerCase() || '';
  }

  formatCurrency(amount?: number): string {
    if (amount === undefined || amount === null) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }
}
