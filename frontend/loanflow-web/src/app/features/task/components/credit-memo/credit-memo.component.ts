import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { TaskResponse, getTaskLabel, formatLoanType, formatCurrency } from '../../models/task.model';
import { LoanApplication, LOAN_TYPES, LOAN_STATUSES } from '../../../loan/models/loan.model';

@Component({
  selector: 'app-credit-memo',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule
  ],
  templateUrl: './credit-memo.component.html',
  styleUrls: ['./credit-memo.component.scss']
})
export class CreditMemoComponent {
  @Input() task!: TaskResponse;
  @Input() loan!: LoanApplication;
  today = new Date().toISOString();

  printMemo(): void {
    window.print();
  }

  getTaskLabel(key: string): string {
    return getTaskLabel(key);
  }

  formatLoanType(type: string): string {
    return formatLoanType(type);
  }

  formatCurrency(amount: string | number | undefined): string {
    if (amount === undefined || amount === null) return '-';
    return formatCurrency(amount);
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
      day: 'numeric'
    });
  }

  getLoanTypeLabel(type?: string): string {
    const typeInfo = LOAN_TYPES.find(t => t.value === type);
    return typeInfo?.label || type || 'Unknown';
  }

  getStatusLabel(status?: string): string {
    const statusInfo = LOAN_STATUSES.find(s => s.value === status);
    return statusInfo?.label || status || 'Unknown';
  }

  getCibilClass(score?: number): string {
    if (!score) return '';
    if (score >= 750) return 'cibil-good';
    if (score >= 650) return 'cibil-fair';
    return 'cibil-poor';
  }

  getCibilRating(score?: number): string {
    if (!score) return 'N/A';
    if (score >= 750) return 'Excellent';
    if (score >= 700) return 'Good';
    if (score >= 650) return 'Fair';
    if (score >= 600) return 'Below Average';
    return 'Poor';
  }
}
