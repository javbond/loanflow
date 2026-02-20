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
import { TaskService } from '../../services/task.service';
import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication, LOAN_STATUSES, LOAN_TYPES } from '../../../loan/models/loan.model';
import { TaskResponse, getTaskLabel, formatLoanType, formatCurrency, CompleteTaskRequest } from '../../models/task.model';
import { DecisionPanelComponent } from '../decision-panel/decision-panel.component';
import { CreditMemoComponent } from '../credit-memo/credit-memo.component';

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
    DecisionPanelComponent,
    CreditMemoComponent
  ],
  templateUrl: './task-detail.component.html',
  styleUrls: ['./task-detail.component.scss']
})
export class TaskDetailComponent implements OnInit {
  task: TaskResponse | null = null;
  loan: LoanApplication | null = null;
  loading = true;
  actionLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private taskService: TaskService,
    private loanService: LoanService,
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
}
