import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { LoanService } from '../../services/loan.service';
import { LoanApplication, LOAN_STATUSES, LOAN_TYPES } from '../../models/loan.model';

@Component({
  selector: 'app-loan-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatListModule,
    MatDividerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './loan-detail.component.html',
  styleUrl: './loan-detail.component.scss'
})
export class LoanDetailComponent implements OnInit {
  loan: LoanApplication | null = null;
  loading = true;
  actionLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private loanService: LoanService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadLoan(id);
    }
  }

  loadLoan(id: string): void {
    this.loading = true;
    this.loanService.getById(id).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load loan application', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/loans']);
      }
    });
  }

  getStatusColor(status?: string): string {
    const statusInfo = LOAN_STATUSES.find(s => s.value === status);
    return statusInfo?.color || 'accent';
  }

  getStatusLabel(status?: string): string {
    const statusInfo = LOAN_STATUSES.find(s => s.value === status);
    return statusInfo?.label || status || 'Unknown';
  }

  getLoanTypeLabel(type?: string): string {
    const typeInfo = LOAN_TYPES.find(t => t.value === type);
    return typeInfo?.label || type || 'Unknown';
  }

  formatAmount(amount?: number): string {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateString?: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  isEditable(): boolean {
    return this.loan?.status === 'DRAFT' || this.loan?.status === 'RETURNED';
  }

  canSubmit(): boolean {
    return this.loan?.status === 'DRAFT' || this.loan?.status === 'RETURNED';
  }

  canApprove(): boolean {
    return this.loan?.status === 'UNDERWRITING' ||
           this.loan?.status === 'CONDITIONALLY_APPROVED' ||
           this.loan?.status === 'REFERRED';
  }

  canReject(): boolean {
    return !['REJECTED', 'CANCELLED', 'DISBURSED', 'CLOSED'].includes(this.loan?.status || '');
  }

  submitApplication(): void {
    if (!this.loan?.id) return;

    this.actionLoading = true;
    this.loanService.submit(this.loan.id).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.snackBar.open('Application submitted successfully', 'Close', { duration: 3000 });
        this.actionLoading = false;
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Submission failed', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  approveApplication(): void {
    if (!this.loan?.id) return;

    // For demo, using requested amount and default rate
    const approvedAmount = this.loan.requestedAmount;
    const interestRate = 10.5;

    this.actionLoading = true;
    this.loanService.approve(this.loan.id, approvedAmount, interestRate).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.snackBar.open('Application approved successfully', 'Close', { duration: 3000 });
        this.actionLoading = false;
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Approval failed', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  rejectApplication(): void {
    if (!this.loan?.id) return;

    const reason = prompt('Enter rejection reason:');
    if (!reason) return;

    this.actionLoading = true;
    this.loanService.reject(this.loan.id, reason).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.snackBar.open('Application rejected', 'Close', { duration: 3000 });
        this.actionLoading = false;
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Rejection failed', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  transitionStatus(newStatus: string): void {
    if (!this.loan?.id) return;

    this.actionLoading = true;
    this.loanService.transitionStatus(this.loan.id, newStatus as any).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.snackBar.open(`Status changed to ${this.getStatusLabel(newStatus)}`, 'Close', { duration: 3000 });
        this.actionLoading = false;
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Status change failed', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  getNextStatuses(): string[] {
    // Based on current status, return allowed transitions
    const transitions: { [key: string]: string[] } = {
      'DRAFT': ['SUBMITTED'],
      'SUBMITTED': ['DOCUMENT_VERIFICATION', 'RETURNED'],
      'DOCUMENT_VERIFICATION': ['CREDIT_CHECK', 'RETURNED', 'REJECTED'],
      'CREDIT_CHECK': ['UNDERWRITING', 'REJECTED'],
      'UNDERWRITING': ['APPROVED', 'CONDITIONALLY_APPROVED', 'REJECTED', 'REFERRED'],
      'CONDITIONALLY_APPROVED': ['APPROVED', 'REJECTED'],
      'REFERRED': ['APPROVED', 'REJECTED'],
      'APPROVED': ['DISBURSEMENT_PENDING'],
      'DISBURSEMENT_PENDING': ['DISBURSED']
    };

    return transitions[this.loan?.status || ''] || [];
  }

  // US-023: Generate Sanction Letter PDF
  generateSanctionLetter(): void {
    if (!this.loan?.id) return;

    this.actionLoading = true;
    this.loanService.generateSanctionLetter(this.loan.id).subscribe({
      next: (blob) => {
        // Download the PDF
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `sanction-letter-${this.loan!.applicationNumber || this.loan!.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Sanction letter downloaded', 'Close', { duration: 3000 });
        this.actionLoading = false;
      },
      error: (error) => {
        this.snackBar.open(
          error.error?.message || 'Failed to generate sanction letter',
          'Close',
          { duration: 3000 }
        );
        this.actionLoading = false;
      }
    });
  }
}
