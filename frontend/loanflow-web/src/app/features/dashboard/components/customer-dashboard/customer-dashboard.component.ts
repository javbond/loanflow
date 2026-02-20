import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { UserInfo } from '../../../../core/auth/models/auth.model';
import { LoanService } from '../../../loan/services/loan.service';
import { DocumentService } from '../../../document/services/document.service';
import { LoanApplication, LOAN_TYPES, LOAN_STATUSES } from '../../../loan/models/loan.model';

/**
 * Customer Dashboard Component
 * Landing page for customers to view their loan application status
 * Updated for Phase 3: Now shows actual applications with offer status
 * Issue: #28 [US-026] Loan Offer Accept/Reject
 */
@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule
  ],
  template: `
    <div class="dashboard-container">
      <div class="welcome-section">
        <h1>Welcome, {{ user?.firstName || 'Customer' }}!</h1>
        <p class="subtitle">Your LoanFlow Customer Portal</p>
      </div>

      <!-- Quick Actions -->
      <div class="quick-actions">
        <button mat-raised-button color="primary" routerLink="/my-portal/apply" class="action-btn">
          <mat-icon>add_circle</mat-icon>
          Apply for a Loan
        </button>
        <button mat-raised-button color="accent" routerLink="/my-portal/documents/upload" class="action-btn">
          <mat-icon>cloud_upload</mat-icon>
          Upload Documents
        </button>
      </div>

      <!-- Approved Offers Alert -->
      @if (approvedApplications.length > 0) {
        <mat-card class="alert-card offer-alert">
          <mat-card-content>
            <div class="alert-content">
              <mat-icon class="alert-icon">celebration</mat-icon>
              <div class="alert-text">
                <strong>You have {{ approvedApplications.length }} approved loan offer{{ approvedApplications.length > 1 ? 's' : '' }}!</strong>
                <p>Review and accept to proceed with disbursement.</p>
              </div>
              <button mat-raised-button color="primary" [routerLink]="['/my-portal/applications', approvedApplications[0].id]">
                View Offer
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      }

      <div class="dashboard-grid">
        <!-- Application Status Card -->
        <mat-card class="dashboard-card status-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon">assignment</mat-icon>
            <mat-card-title>My Loan Applications</mat-card-title>
            <mat-card-subtitle>Track your application status</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (loadingApplications) {
              <div class="loading-content">
                <mat-spinner diameter="32"></mat-spinner>
              </div>
            } @else if (applications.length === 0) {
              <div class="status-placeholder">
                <mat-icon class="placeholder-icon">inbox</mat-icon>
                <p>No active applications</p>
                <p class="hint">Click "Apply for a Loan" above to get started</p>
              </div>
            } @else {
              <div class="applications-summary">
                @for (app of applications.slice(0, 3); track app.id) {
                  <div class="app-item" [routerLink]="['/my-portal/applications', app.id]">
                    <div class="app-info">
                      <span class="app-type">{{ getLoanTypeLabel(app.loanType) }}</span>
                      <span class="app-number">{{ app.applicationNumber }}</span>
                    </div>
                    <div class="app-status">
                      <mat-chip [class]="'status-chip ' + app.status?.toLowerCase()" size="small">
                        {{ getStatusLabel(app.status) }}
                      </mat-chip>
                      @if (app.status === 'APPROVED') {
                        <mat-icon class="offer-indicator">star</mat-icon>
                      }
                    </div>
                  </div>
                }
                @if (applications.length > 3) {
                  <p class="more-hint">+{{ applications.length - 3 }} more applications</p>
                }
              </div>
            }
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/my-portal/applications">
              <mat-icon>list</mat-icon> View All Applications
            </button>
          </mat-card-actions>
        </mat-card>

        <!-- Documents Card -->
        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon documents-icon">folder_open</mat-icon>
            <mat-card-title>My Documents</mat-card-title>
            <mat-card-subtitle>{{ documentCount }} documents uploaded</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (loadingDocuments) {
              <div class="loading-content">
                <mat-spinner diameter="32"></mat-spinner>
              </div>
            } @else if (documentCount === 0) {
              <div class="status-placeholder">
                <mat-icon class="placeholder-icon">cloud_upload</mat-icon>
                <p>No documents uploaded</p>
                <p class="hint">Documents will appear here once uploaded</p>
              </div>
            } @else {
              <div class="documents-summary">
                <div class="stat-box">
                  <span class="stat-value">{{ documentCount }}</span>
                  <span class="stat-label">Total Documents</span>
                </div>
              </div>
            }
          </mat-card-content>
          <mat-card-actions>
            <button mat-button color="primary" routerLink="/my-portal/documents/upload">
              <mat-icon>upload</mat-icon> Upload Document
            </button>
          </mat-card-actions>
        </mat-card>

        <!-- Summary Stats Card -->
        <mat-card class="dashboard-card stats-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon stats-icon">analytics</mat-icon>
            <mat-card-title>Application Summary</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-number">{{ applications.length }}</span>
                <span class="stat-label">Total Applications</span>
              </div>
              <div class="stat-item active">
                <span class="stat-number">{{ activeApplications.length }}</span>
                <span class="stat-label">In Progress</span>
              </div>
              <div class="stat-item approved">
                <span class="stat-number">{{ approvedApplications.length }}</span>
                <span class="stat-label">Approved</span>
              </div>
              <div class="stat-item disbursed">
                <span class="stat-number">{{ disbursedApplications.length }}</span>
                <span class="stat-label">Disbursed</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Contact Card -->
        <mat-card class="dashboard-card contact-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon contact-icon">support_agent</mat-icon>
            <mat-card-title>Need Help?</mat-card-title>
            <mat-card-subtitle>Contact your loan officer</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="contact-info">
              <p><mat-icon inline>email</mat-icon> support&#64;loanflow.com</p>
              <p><mat-icon inline>phone</mat-icon> 1800-LOAN-FLOW</p>
              <p><mat-icon inline>schedule</mat-icon> Mon-Fri: 9 AM - 6 PM</p>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }

    .welcome-section {
      margin-bottom: 32px;
    }

    .welcome-section h1 {
      margin: 0;
      font-size: 2rem;
      color: #1976d2;
    }

    .subtitle {
      color: #666;
      margin: 8px 0 0;
    }

    .quick-actions {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }

    .action-btn {
      font-size: 1rem;
      padding: 12px 24px;
    }

    .action-btn mat-icon {
      margin-right: 8px;
    }

    /* Alert Card */
    .alert-card {
      margin-bottom: 24px;
    }

    .offer-alert {
      background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
      border-left: 4px solid #4caf50;
    }

    .alert-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .alert-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #4caf50;
    }

    .alert-text {
      flex: 1;
    }

    .alert-text strong {
      color: #2e7d32;
    }

    .alert-text p {
      margin: 4px 0 0;
      color: #666;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 24px;
    }

    .dashboard-card {
      height: 100%;
    }

    .card-icon {
      background: #1976d2;
      color: white;
      border-radius: 50%;
      padding: 8px;
      font-size: 24px;
      width: 40px;
      height: 40px;
    }

    .documents-icon { background: #ff9800; }
    .contact-icon { background: #4caf50; }
    .stats-icon { background: #9c27b0; }

    .loading-content {
      display: flex;
      justify-content: center;
      padding: 24px;
    }

    .status-placeholder {
      text-align: center;
      padding: 24px;
      color: #666;
    }

    .placeholder-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #ccc;
    }

    .hint {
      font-size: 0.875rem;
      color: #999;
    }

    /* Applications Summary */
    .applications-summary {
      padding: 8px 0;
    }

    .app-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      border-radius: 8px;
      margin-bottom: 8px;
      background: #f5f5f5;
      cursor: pointer;
      transition: background 0.2s;
    }

    .app-item:hover {
      background: #e3f2fd;
    }

    .app-info {
      display: flex;
      flex-direction: column;
    }

    .app-type {
      font-weight: 500;
      color: #333;
    }

    .app-number {
      font-size: 0.8rem;
      color: #666;
      font-family: monospace;
    }

    .app-status {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .offer-indicator {
      color: #ff9800;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .more-hint {
      text-align: center;
      color: #666;
      font-size: 0.875rem;
      margin: 8px 0 0;
    }

    .status-chip {
      font-size: 0.75rem;
    }

    .status-chip.submitted { background: #e3f2fd; color: #1565c0; }
    .status-chip.document_verification { background: #fff3e0; color: #e65100; }
    .status-chip.credit_check { background: #f3e5f5; color: #7b1fa2; }
    .status-chip.underwriting { background: #e8eaf6; color: #3949ab; }
    .status-chip.approved { background: #e8f5e9; color: #2e7d32; }
    .status-chip.disbursement_pending { background: #e1f5fe; color: #0277bd; }
    .status-chip.disbursed { background: #e0f2f1; color: #00695c; }
    .status-chip.rejected { background: #ffebee; color: #c62828; }

    /* Documents Summary */
    .documents-summary {
      display: flex;
      justify-content: center;
      padding: 16px;
    }

    .stat-box {
      text-align: center;
      padding: 16px 32px;
      background: #e3f2fd;
      border-radius: 12px;
    }

    .stat-box .stat-value {
      display: block;
      font-size: 2rem;
      font-weight: 600;
      color: #1976d2;
    }

    .stat-box .stat-label {
      color: #666;
    }

    /* Stats Grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
      padding: 16px 0;
    }

    .stat-item {
      text-align: center;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .stat-item.active { background: #e3f2fd; }
    .stat-item.approved { background: #e8f5e9; }
    .stat-item.disbursed { background: #e0f2f1; }

    .stat-number {
      display: block;
      font-size: 1.75rem;
      font-weight: 600;
      color: #333;
    }

    .stat-item.active .stat-number { color: #1976d2; }
    .stat-item.approved .stat-number { color: #4caf50; }
    .stat-item.disbursed .stat-number { color: #00897b; }

    .stat-label {
      font-size: 0.875rem;
      color: #666;
    }

    /* Contact Info */
    .contact-info {
      padding: 16px 0;
    }

    .contact-info p {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 12px 0;
      color: #333;
    }

    .contact-info mat-icon {
      color: #1976d2;
    }

    mat-card-actions {
      padding: 8px 16px !important;
    }

    @media (max-width: 600px) {
      .alert-content {
        flex-direction: column;
        text-align: center;
      }

      .stats-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CustomerDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private loanService = inject(LoanService);
  private documentService = inject(DocumentService);

  user: UserInfo | null = null;
  applications: LoanApplication[] = [];
  loadingApplications = true;
  loadingDocuments = true;
  documentCount = 0;

  ngOnInit(): void {
    this.user = this.authService.getCurrentUserSync();
    this.loadApplications();
    this.loadDocuments();
  }

  loadApplications(): void {
    this.loadingApplications = true;
    this.loanService.getMyApplications(0, 50).subscribe({
      next: (response) => {
        this.applications = response.content || [];
        this.loadingApplications = false;
      },
      error: () => {
        this.loadingApplications = false;
        this.applications = [];
      }
    });
  }

  loadDocuments(): void {
    this.loadingDocuments = true;
    this.documentService.getMyDocuments(0, 1).subscribe({
      next: (response) => {
        this.documentCount = response.totalElements || 0;
        this.loadingDocuments = false;
      },
      error: () => {
        this.loadingDocuments = false;
        this.documentCount = 0;
      }
    });
  }

  get approvedApplications(): LoanApplication[] {
    return this.applications.filter(a => a.status === 'APPROVED');
  }

  get activeApplications(): LoanApplication[] {
    const activeStatuses = ['SUBMITTED', 'DOCUMENT_VERIFICATION', 'CREDIT_CHECK', 'UNDERWRITING', 'CONDITIONALLY_APPROVED', 'REFERRED', 'DISBURSEMENT_PENDING'];
    return this.applications.filter(a => a.status && activeStatuses.includes(a.status));
  }

  get disbursedApplications(): LoanApplication[] {
    return this.applications.filter(a => a.status === 'DISBURSED' || a.status === 'CLOSED');
  }

  getLoanTypeLabel(type: string): string {
    return LOAN_TYPES.find(t => t.value === type)?.label || type;
  }

  getStatusLabel(status?: string): string {
    return LOAN_STATUSES.find(s => s.value === status)?.label || status || 'Unknown';
  }
}
