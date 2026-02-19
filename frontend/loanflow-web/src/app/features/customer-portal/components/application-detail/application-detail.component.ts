import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { STEPPER_GLOBAL_OPTIONS } from '@angular/cdk/stepper';

import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication, LOAN_TYPES, LOAN_STATUSES, LoanStatus } from '../../../loan/models/loan.model';

/**
 * Application Detail Component
 * Shows application details, status timeline, and loan offer (if approved)
 * Issue: #28 [US-026] Loan Offer Accept/Reject
 */
@Component({
  selector: 'app-application-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatStepperModule,
    MatSnackBarModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  providers: [
    { provide: STEPPER_GLOBAL_OPTIONS, useValue: { displayDefaultIndicatorType: false } }
  ],
  template: `
    <div class="detail-container">
      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading application details...</p>
        </div>
      } @else if (!application) {
        <mat-card class="error-card">
          <mat-card-content>
            <div class="error-state">
              <mat-icon class="error-icon">error_outline</mat-icon>
              <h2>Application Not Found</h2>
              <p>The application you're looking for doesn't exist or you don't have access to it.</p>
              <button mat-raised-button color="primary" routerLink="/my-portal/applications">
                <mat-icon>arrow_back</mat-icon> Back to My Applications
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      } @else {
        <!-- Header -->
        <div class="header-section">
          <button mat-icon-button routerLink="/my-portal/applications" class="back-btn">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <div class="title-info">
            <h1>{{ getLoanTypeLabel(application.loanType) }}</h1>
            <p class="app-number">{{ application.applicationNumber }}</p>
          </div>
          <mat-chip [class]="'status-chip ' + application.status?.toLowerCase()">
            {{ getStatusLabel(application.status) }}
          </mat-chip>
        </div>

        <!-- Loan Offer Card (only for APPROVED status) -->
        @if (application.status === 'APPROVED') {
          <mat-card class="offer-card">
            <mat-card-header>
              <mat-icon mat-card-avatar class="offer-icon">celebration</mat-icon>
              <mat-card-title>Congratulations! Your Loan is Approved</mat-card-title>
              <mat-card-subtitle>Review your loan offer and accept to proceed with disbursement</mat-card-subtitle>
            </mat-card-header>

            <mat-card-content>
              <div class="offer-details">
                <div class="offer-highlight">
                  <div class="offer-item main-amount">
                    <span class="offer-label">Approved Amount</span>
                    <span class="offer-value">{{ formatCurrency(application.approvedAmount) }}</span>
                  </div>
                  <div class="offer-item">
                    <span class="offer-label">Interest Rate</span>
                    <span class="offer-value">{{ application.interestRate }}% p.a.</span>
                  </div>
                  <div class="offer-item">
                    <span class="offer-label">Tenure</span>
                    <span class="offer-value">{{ application.tenureMonths }} months</span>
                  </div>
                  <div class="offer-item emi-highlight">
                    <span class="offer-label">Monthly EMI</span>
                    <span class="offer-value emi">{{ formatCurrency(application.emiAmount) }}</span>
                  </div>
                </div>

                <mat-divider></mat-divider>

                <!-- Amortization Summary -->
                <div class="amortization-summary">
                  <h3><mat-icon>calculate</mat-icon> Loan Summary</h3>
                  <div class="summary-grid">
                    <div class="summary-item">
                      <span class="label">Principal Amount</span>
                      <span class="value">{{ formatCurrency(application.approvedAmount) }}</span>
                    </div>
                    <div class="summary-item">
                      <span class="label">Total Interest</span>
                      <span class="value">{{ formatCurrency(calculateTotalInterest()) }}</span>
                    </div>
                    <div class="summary-item">
                      <span class="label">Total Payable</span>
                      <span class="value total">{{ formatCurrency(calculateTotalPayable()) }}</span>
                    </div>
                    @if (application.processingFee) {
                      <div class="summary-item">
                        <span class="label">Processing Fee</span>
                        <span class="value">{{ formatCurrency(application.processingFee) }}</span>
                      </div>
                    }
                  </div>
                </div>

                <mat-divider></mat-divider>

                @if (application.expectedDisbursementDate) {
                  <div class="disbursement-info">
                    <mat-icon>event</mat-icon>
                    <span>Expected Disbursement: <strong>{{ formatDate(application.expectedDisbursementDate) }}</strong></span>
                  </div>
                }
              </div>
            </mat-card-content>

            <mat-card-actions class="offer-actions">
              @if (!showRejectForm) {
                <button mat-raised-button color="primary" (click)="acceptOffer()" [disabled]="processing" class="accept-btn">
                  @if (processing) {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>check_circle</mat-icon> Accept Offer
                  }
                </button>
                <button mat-stroked-button color="warn" (click)="showRejectForm = true" [disabled]="processing">
                  <mat-icon>cancel</mat-icon> Decline Offer
                </button>
              } @else {
                <form [formGroup]="rejectForm" (ngSubmit)="rejectOffer()" class="reject-form">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Reason for declining</mat-label>
                    <textarea matInput formControlName="reason" rows="3"
                              placeholder="Please tell us why you're declining this offer..."></textarea>
                    <mat-error *ngIf="rejectForm.get('reason')?.hasError('required')">
                      Please provide a reason
                    </mat-error>
                  </mat-form-field>
                  <div class="reject-actions">
                    <button mat-button type="button" (click)="showRejectForm = false" [disabled]="processing">
                      Cancel
                    </button>
                    <button mat-raised-button color="warn" type="submit"
                            [disabled]="rejectForm.invalid || processing">
                      @if (processing) {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>thumb_down</mat-icon> Confirm Decline
                      }
                    </button>
                  </div>
                </form>
              }
            </mat-card-actions>
          </mat-card>
        }

        <!-- Status Timeline -->
        <mat-card class="timeline-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="timeline-icon">timeline</mat-icon>
            <mat-card-title>Application Status</mat-card-title>
            <mat-card-subtitle>Track your application progress</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="status-timeline">
              @for (step of statusSteps; track step.status; let i = $index) {
                <div class="timeline-step" [class.active]="isStepActive(step.status)"
                     [class.completed]="isStepCompleted(step.status)"
                     [class.current]="isCurrentStep(step.status)">
                  <div class="step-indicator">
                    @if (isStepCompleted(step.status)) {
                      <mat-icon>check_circle</mat-icon>
                    } @else if (isCurrentStep(step.status)) {
                      <mat-icon>radio_button_checked</mat-icon>
                    } @else {
                      <mat-icon>radio_button_unchecked</mat-icon>
                    }
                    @if (i < statusSteps.length - 1) {
                      <div class="step-connector" [class.filled]="isStepCompleted(step.status)"></div>
                    }
                  </div>
                  <div class="step-content">
                    <span class="step-label">{{ step.label }}</span>
                    @if (isCurrentStep(step.status)) {
                      <span class="current-badge">Current</span>
                    }
                  </div>
                </div>
              }
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Application Details -->
        <mat-card class="details-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="details-icon">description</mat-icon>
            <mat-card-title>Application Details</mat-card-title>
          </mat-card-header>

          <mat-card-content>
            <div class="details-grid">
              <div class="detail-section">
                <h4>Loan Information</h4>
                <div class="detail-row">
                  <span class="label">Loan Type</span>
                  <span class="value">{{ getLoanTypeLabel(application.loanType) }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Requested Amount</span>
                  <span class="value">{{ formatCurrency(application.requestedAmount) }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Tenure</span>
                  <span class="value">{{ application.tenureMonths }} months ({{ (application.tenureMonths / 12) | number:'1.1-1' }} years)</span>
                </div>
                @if (application.purpose) {
                  <div class="detail-row">
                    <span class="label">Purpose</span>
                    <span class="value">{{ application.purpose }}</span>
                  </div>
                }
              </div>

              <div class="detail-section">
                <h4>Application Info</h4>
                <div class="detail-row">
                  <span class="label">Application Number</span>
                  <span class="value mono">{{ application.applicationNumber }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Applied On</span>
                  <span class="value">{{ formatDate(application.createdAt) }}</span>
                </div>
                @if (application.submittedAt) {
                  <div class="detail-row">
                    <span class="label">Submitted On</span>
                    <span class="value">{{ formatDate(application.submittedAt) }}</span>
                  </div>
                }
                @if (application.cibilScore) {
                  <div class="detail-row">
                    <span class="label">Credit Score</span>
                    <span class="value" [class]="getCibilClass(application.cibilScore)">{{ application.cibilScore }}</span>
                  </div>
                }
              </div>
            </div>
          </mat-card-content>

          <mat-card-actions>
            <button mat-button color="primary" [routerLink]="['/my-portal/documents/upload', application.id]">
              <mat-icon>upload_file</mat-icon> Upload Documents
            </button>
          </mat-card-actions>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .detail-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .loading-container, .error-state { display: flex; flex-direction: column; align-items: center; padding: 48px; text-align: center; }
    .error-icon { font-size: 72px; width: 72px; height: 72px; color: #f44336; }
    .header-section { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .back-btn { flex-shrink: 0; }
    .title-info { flex: 1; }
    .title-info h1 { margin: 0; font-size: 1.5rem; color: #333; }
    .app-number { margin: 4px 0 0; color: #666; font-family: monospace; }
    .status-chip { font-weight: 500; flex-shrink: 0; }

    .status-chip.submitted { background: #e3f2fd; color: #1565c0; }
    .status-chip.approved { background: #e8f5e9; color: #2e7d32; }
    .status-chip.disbursed { background: #e0f2f1; color: #00695c; }
    .status-chip.rejected { background: #ffebee; color: #c62828; }
    .offer-card { margin-bottom: 24px; border: 2px solid #4caf50; background: linear-gradient(135deg, #fff 0%, #e8f5e9 100%); }
    .offer-icon, .timeline-icon, .details-icon { background: #4caf50; color: white; border-radius: 50%; padding: 8px; font-size: 28px; width: 44px; height: 44px; }
    .timeline-icon { background: #1976d2; }
    .details-icon { background: #ff9800; }
    .offer-details, .status-timeline { padding: 16px 0; }
    .offer-highlight { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 24px; margin-bottom: 24px; }
    .offer-item { text-align: center; padding: 16px; background: white; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .offer-item.main-amount { grid-column: span 2; }
    .offer-item.emi-highlight { background: linear-gradient(135deg, #ff9800, #ffb74d); }
    .offer-item.emi-highlight .offer-label, .offer-item.emi-highlight .offer-value { color: white; }
    .offer-label { display: block; font-size: 0.875rem; color: #666; margin-bottom: 8px; }
    .offer-value { font-size: 1.5rem; font-weight: 600; color: #2e7d32; }
    .offer-value.emi { color: white; }
    .amortization-summary { padding: 16px 0; }
    .amortization-summary h3 { display: flex; align-items: center; gap: 8px; margin: 0 0 16px; font-size: 1rem; }
    .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
    .summary-item { display: flex; flex-direction: column; gap: 4px; }
    .summary-item .label { font-size: 0.875rem; color: #666; }
    .summary-item .value { font-size: 1.1rem; font-weight: 500; }
    .summary-item .value.total { color: #1976d2; font-weight: 600; }
    .disbursement-info { display: flex; align-items: center; gap: 12px; padding: 16px; background: #e3f2fd; border-radius: 8px; margin-top: 16px; }
    .disbursement-info mat-icon { color: #1976d2; }
    .offer-actions { display: flex; flex-direction: column; gap: 16px; padding: 16px !important; }
    .accept-btn { padding: 12px 32px; font-size: 1rem; }
    .reject-form { width: 100%; }
    .reject-form .full-width { width: 100%; }
    .reject-actions { display: flex; justify-content: flex-end; gap: 12px; }
    .timeline-card, .details-card { margin-bottom: 24px; }
    .timeline-step { display: flex; gap: 16px; padding: 8px 0; }
    .step-indicator { display: flex; flex-direction: column; align-items: center; }

    .step-indicator mat-icon { font-size: 24px; width: 24px; height: 24px; color: #ccc; }
    .step-connector { width: 2px; height: 32px; background: #e0e0e0; margin: 4px 0; }
    .step-connector.filled { background: #4caf50; }
    .timeline-step.completed .step-indicator mat-icon { color: #4caf50; }
    .timeline-step.current .step-indicator mat-icon { color: #1976d2; }
    .step-content { flex: 1; display: flex; align-items: center; gap: 8px; padding-top: 2px; }
    .step-label { color: #666; }
    .timeline-step.completed .step-label, .timeline-step.current .step-label { color: #333; font-weight: 500; }
    .current-badge { font-size: 0.75rem; background: #1976d2; color: white; padding: 2px 8px; border-radius: 12px; }
    .details-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; padding: 16px 0; }
    .detail-section h4 { margin: 0 0 16px; border-bottom: 2px solid #e0e0e0; padding-bottom: 8px; }
    .detail-row { display: flex; justify-content: space-between; padding: 8px 0; }
    .detail-row .label { color: #666; }
    .detail-row .value { font-weight: 500; }
    .detail-row .value.mono { font-family: monospace; }
    .detail-row .value.excellent { color: #2e7d32; }
    .detail-row .value.good { color: #4caf50; }
    .detail-row .value.fair { color: #ff9800; }
    .detail-row .value.poor { color: #f44336; }
  `]
})
export class ApplicationDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private loanService = inject(LoanService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  application: LoanApplication | null = null;
  loading = true;
  processing = false;
  showRejectForm = false;

  rejectForm: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]]
  });

  // Status steps for timeline
  statusSteps: { status: LoanStatus; label: string }[] = [
    { status: 'SUBMITTED', label: 'Application Submitted' },
    { status: 'DOCUMENT_VERIFICATION', label: 'Document Verification' },
    { status: 'CREDIT_CHECK', label: 'Credit Check' },
    { status: 'UNDERWRITING', label: 'Underwriting Review' },
    { status: 'APPROVED', label: 'Loan Approved' },
    { status: 'DISBURSEMENT_PENDING', label: 'Disbursement Pending' },
    { status: 'DISBURSED', label: 'Loan Disbursed' }
  ];

  // Status order for comparison
  statusOrder: LoanStatus[] = [
    'DRAFT', 'SUBMITTED', 'DOCUMENT_VERIFICATION', 'CREDIT_CHECK',
    'UNDERWRITING', 'CONDITIONALLY_APPROVED', 'REFERRED', 'APPROVED',
    'DISBURSEMENT_PENDING', 'DISBURSED', 'CLOSED'
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadApplication(id);
    } else {
      this.loading = false;
    }
  }

  loadApplication(id: string): void {
    this.loading = true;
    this.loanService.getById(id).subscribe({
      next: (response) => {
        this.application = response.data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.application = null;
      }
    });
  }

  acceptOffer(): void {
    if (!this.application?.id) return;

    this.processing = true;
    this.loanService.acceptOffer(this.application.id).subscribe({
      next: (response) => {
        this.application = response.data;
        this.processing = false;
        this.snackBar.open('Congratulations! Your loan offer has been accepted. Disbursement will be processed soon.', 'Close', {
          duration: 5000
        });
      },
      error: (error) => {
        this.processing = false;
        this.snackBar.open(error.error?.message || 'Failed to accept offer. Please try again.', 'Close', {
          duration: 5000
        });
      }
    });
  }

  rejectOffer(): void {
    if (!this.application?.id || this.rejectForm.invalid) return;

    this.processing = true;
    const reason = this.rejectForm.value.reason;

    this.loanService.rejectOffer(this.application.id, reason).subscribe({
      next: () => {
        this.processing = false;
        this.snackBar.open('Your loan offer has been declined.', 'Close', {
          duration: 3000
        });
        this.router.navigate(['/my-portal/applications']);
      },
      error: (error) => {
        this.processing = false;
        this.snackBar.open(error.error?.message || 'Failed to decline offer. Please try again.', 'Close', {
          duration: 5000
        });
      }
    });
  }

  isStepCompleted(status: LoanStatus): boolean {
    if (!this.application?.status) return false;
    const currentIndex = this.statusOrder.indexOf(this.application.status);
    const stepIndex = this.statusOrder.indexOf(status);
    return stepIndex < currentIndex;
  }

  isCurrentStep(status: LoanStatus): boolean {
    return this.application?.status === status;
  }

  isStepActive(status: LoanStatus): boolean {
    return this.isStepCompleted(status) || this.isCurrentStep(status);
  }

  getLoanTypeLabel(type?: string): string {
    if (!type) return 'Unknown';
    return LOAN_TYPES.find(t => t.value === type)?.label || type;
  }

  getStatusLabel(status?: string): string {
    if (!status) return 'Unknown';
    return LOAN_STATUSES.find(s => s.value === status)?.label || status;
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
      month: 'long',
      year: 'numeric'
    });
  }

  calculateTotalInterest(): number {
    if (!this.application?.emiAmount || !this.application?.approvedAmount || !this.application?.tenureMonths) {
      return 0;
    }
    const totalPayable = this.application.emiAmount * this.application.tenureMonths;
    return totalPayable - this.application.approvedAmount;
  }

  calculateTotalPayable(): number {
    if (!this.application?.emiAmount || !this.application?.tenureMonths) {
      return 0;
    }
    return this.application.emiAmount * this.application.tenureMonths;
  }

  getCibilClass(score?: number): string {
    if (!score) return '';
    if (score >= 750) return 'excellent';
    if (score >= 700) return 'good';
    if (score >= 650) return 'fair';
    return 'poor';
  }
}
