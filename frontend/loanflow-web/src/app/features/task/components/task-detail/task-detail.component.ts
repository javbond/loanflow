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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTableModule } from '@angular/material/table';
import { TaskService } from '../../services/task.service';
import { CreditBureauService } from '../../services/credit-bureau.service';
import { IncomeVerificationService } from '../../services/income-verification.service';
import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication, LOAN_STATUSES, LOAN_TYPES } from '../../../loan/models/loan.model';
import { TaskResponse, getTaskLabel, formatLoanType, formatCurrency, CompleteTaskRequest } from '../../models/task.model';
import {
  CreditBureauResponse,
  BureauDataSource,
  BUREAU_SOURCE_COLORS,
  BUREAU_SOURCE_LABELS
} from '../../models/credit-bureau.model';
import {
  IncomeVerificationResponse,
  IncomeDataSource,
  INCOME_SOURCE_COLORS,
  INCOME_SOURCE_LABELS,
  GST_COMPLIANCE_COLORS
} from '../../models/income-verification.model';
import { DecisionPanelComponent } from '../decision-panel/decision-panel.component';
import { CreditMemoComponent } from '../credit-memo/credit-memo.component';
import { DocumentPanelComponent } from '../../../document/components/document-panel/document-panel.component';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatListModule,
    MatDividerModule,
    MatTooltipModule,
    MatExpansionModule,
    MatTableModule,
    DecisionPanelComponent,
    CreditMemoComponent,
    DocumentPanelComponent
  ],
  templateUrl: './task-detail.component.html',
  styleUrls: ['./task-detail.component.scss']
})
export class TaskDetailComponent implements OnInit {
  task: TaskResponse | null = null;
  loan: LoanApplication | null = null;
  bureauReport: CreditBureauResponse | null = null;
  bureauLoading = false;
  incomeReport: IncomeVerificationResponse | null = null;
  incomeLoading = false;
  loading = true;
  actionLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private taskService: TaskService,
    private loanService: LoanService,
    private creditBureauService: CreditBureauService,
    private incomeVerificationService: IncomeVerificationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const taskId = this.route.snapshot.paramMap.get('taskId');
    if (taskId) {
      this.loadTask(taskId);
    }
  }

  loadTask(taskId: string): void {
    this.loading = true;
    this.taskService.getTask(taskId).subscribe({
      next: (response) => {
        this.task = response.data;
        // Load full loan application for context
        if (this.task?.applicationId) {
          this.loadLoanApplication(this.task.applicationId);
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.snackBar.open('Failed to load task details', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/tasks']);
      }
    });
  }

  loadLoanApplication(applicationId: string): void {
    this.loanService.getById(applicationId).subscribe({
      next: (response) => {
        this.loan = response.data;
        this.loading = false;
      },
      error: () => {
        // Loan data is supplementary — proceed with task data only
        this.snackBar.open('Could not load full loan details', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
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

  formatAmount(amount?: number): string {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  getCibilClass(score?: number): string {
    if (!score) return '';
    if (score >= 750) return 'cibil-good';
    if (score >= 650) return 'cibil-fair';
    return 'cibil-poor';
  }

  getRiskColor(risk: string): string {
    const colors: Record<string, string> = { LOW: 'primary', MEDIUM: 'accent', HIGH: 'warn' };
    return colors[risk] || '';
  }

  isAssignedToMe(): boolean {
    return this.task?.assignee != null;
  }

  claimTask(): void {
    if (!this.task) return;
    this.actionLoading = true;
    this.taskService.claimTask(this.task.taskId).subscribe({
      next: () => {
        this.snackBar.open('Task claimed successfully', 'Close', { duration: 2000 });
        this.loadTask(this.task!.taskId);
        this.actionLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to claim task', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  releaseTask(): void {
    if (!this.task) return;
    this.actionLoading = true;
    this.taskService.unclaimTask(this.task.taskId).subscribe({
      next: () => {
        this.snackBar.open('Task released', 'Close', { duration: 2000 });
        this.router.navigate(['/tasks']);
      },
      error: () => {
        this.snackBar.open('Failed to release task', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  onDecisionSubmitted(request: CompleteTaskRequest): void {
    if (!this.task) return;
    this.actionLoading = true;
    this.taskService.completeTask(this.task.taskId, request).subscribe({
      next: () => {
        const action = request.decision === 'APPROVED' ? 'approved' :
                       request.decision === 'REJECTED' ? 'rejected' : 'referred';
        this.snackBar.open(`Task ${action} successfully`, 'Close', { duration: 3000 });
        this.router.navigate(['/tasks']);
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Failed to complete task', 'Close', { duration: 3000 });
        this.actionLoading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/tasks']);
  }

  // ==================== Credit Bureau Methods ====================

  pullCreditReport(pan: string): void {
    this.bureauLoading = true;
    this.creditBureauService.pullReport({ pan }).subscribe({
      next: (response) => {
        this.bureauReport = response;
        this.bureauLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to pull credit bureau report', 'Close', { duration: 3000 });
        this.bureauLoading = false;
      }
    });
  }

  getBureauSourceColor(source?: string): string {
    if (!source) return '';
    return BUREAU_SOURCE_COLORS[source as BureauDataSource] || '';
  }

  getBureauSourceLabel(source?: string): string {
    if (!source) return '';
    return BUREAU_SOURCE_LABELS[source as BureauDataSource] || source;
  }

  formatBalance(amount?: number): string {
    if (amount === undefined || amount === null) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  // ==================== Income Verification Methods ====================

  pullIncomeReport(pan: string): void {
    this.incomeLoading = true;
    this.incomeVerificationService.verify({ pan }).subscribe({
      next: (response) => {
        this.incomeReport = response;
        this.incomeLoading = false;
      },
      error: () => {
        this.snackBar.open('Failed to pull income verification', 'Close', { duration: 3000 });
        this.incomeLoading = false;
      }
    });
  }

  getIncomeSourceColor(source?: string): string {
    if (!source) return '';
    return INCOME_SOURCE_COLORS[source as IncomeDataSource] || '';
  }

  getIncomeSourceLabel(source?: string): string {
    if (!source) return '';
    return INCOME_SOURCE_LABELS[source as IncomeDataSource] || source;
  }

  getDtiClass(dti?: number): string {
    if (dti === undefined || dti === null) return '';
    if (dti > 0.5) return 'cibil-poor';
    if (dti > 0.4) return 'cibil-fair';
    return 'cibil-good';
  }

  getConsistencyClass(score?: number): string {
    if (score === undefined || score === null) return '';
    if (score >= 90) return 'cibil-good';
    if (score >= 70) return 'cibil-fair';
    return 'cibil-poor';
  }

  getGstComplianceColor(rating?: string): string {
    if (!rating) return '';
    return GST_COMPLIANCE_COLORS[rating] || '';
  }

  formatPercent(value?: number): string {
    if (value === undefined || value === null) return '-';
    return (value * 100).toFixed(1) + '%';
  }
}
