import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { UserInfo } from '../../../../core/auth/models/auth.model';

/**
 * Customer Dashboard Component
 * Landing page for customers to view their loan application status
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
    MatChipsModule
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

      <div class="dashboard-grid">
        <!-- Application Status Card -->
        <mat-card class="dashboard-card status-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon">assignment</mat-icon>
            <mat-card-title>My Loan Applications</mat-card-title>
            <mat-card-subtitle>Track your application status</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="status-placeholder">
              <mat-icon class="placeholder-icon">inbox</mat-icon>
              <p>No active applications</p>
              <p class="hint">Click "Apply for a Loan" above to get started</p>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Documents Card -->
        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon documents-icon">folder_open</mat-icon>
            <mat-card-title>My Documents</mat-card-title>
            <mat-card-subtitle>View uploaded documents</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="status-placeholder">
              <mat-icon class="placeholder-icon">cloud_upload</mat-icon>
              <p>No documents uploaded</p>
              <p class="hint">Documents will appear here once uploaded</p>
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

        <!-- Quick Info Card -->
        <mat-card class="dashboard-card info-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-icon info-icon">info</mat-icon>
            <mat-card-title>Account Information</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="account-info">
              <div class="info-row">
                <span class="label">Email:</span>
                <span class="value">{{ user?.email }}</span>
              </div>
              <div class="info-row">
                <span class="label">Name:</span>
                <span class="value">{{ user?.fullName || 'Customer' }}</span>
              </div>
              <div class="info-row">
                <span class="label">Role:</span>
                <mat-chip-set>
                  <mat-chip *ngFor="let role of user?.roles" color="primary" highlighted>
                    {{ role }}
                  </mat-chip>
                </mat-chip-set>
              </div>
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

    .documents-icon {
      background: #ff9800;
    }

    .contact-icon {
      background: #4caf50;
    }

    .info-icon {
      background: #9c27b0;
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

    .account-info {
      padding: 16px 0;
    }

    .info-row {
      display: flex;
      align-items: center;
      margin: 12px 0;
    }

    .info-row .label {
      font-weight: 500;
      color: #666;
      width: 80px;
    }

    .info-row .value {
      color: #333;
    }

    mat-chip-set {
      display: inline-flex;
    }
  `]
})
export class CustomerDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  user: UserInfo | null = null;

  ngOnInit(): void {
    this.user = this.authService.getCurrentUserSync();
  }
}
