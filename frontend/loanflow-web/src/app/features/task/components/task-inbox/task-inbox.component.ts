import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { TaskService } from '../../services/task.service';
import { TaskResponse, getTaskLabel, formatLoanType, formatCurrency, RISK_COLORS } from '../../models/task.model';

@Component({
  selector: 'app-task-inbox',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './task-inbox.component.html',
  styleUrls: ['./task-inbox.component.scss']
})
export class TaskInboxComponent implements OnInit {
  // Inbox tab
  inboxDataSource = new MatTableDataSource<TaskResponse>();
  inboxTotal = 0;
  inboxPageSize = 10;
  inboxPageIndex = 0;

  // My Tasks tab
  myTasksDataSource = new MatTableDataSource<TaskResponse>();
  myTasksTotal = 0;
  myTasksPageSize = 10;
  myTasksPageIndex = 0;

  displayedColumns = ['taskName', 'applicationNumber', 'loanType', 'requestedAmount', 'cibilScore', 'riskCategory', 'createdAt', 'actions'];

  loading = false;
  activeTab = 0;

  @ViewChild('inboxPaginator') inboxPaginator!: MatPaginator;
  @ViewChild('myTasksPaginator') myTasksPaginator!: MatPaginator;

  constructor(
    private taskService: TaskService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadInbox();
    this.loadMyTasks();
  }

  onTabChange(index: number): void {
    this.activeTab = index;
    if (index === 0) {
      this.loadInbox();
    } else {
      this.loadMyTasks();
    }
  }

  loadInbox(): void {
    this.loading = true;
    this.taskService.getInbox(this.inboxPageIndex, this.inboxPageSize).subscribe({
      next: (page) => {
        this.inboxDataSource.data = page.content;
        this.inboxTotal = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load task inbox', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadMyTasks(): void {
    this.loading = true;
    this.taskService.getMyTasks(this.myTasksPageIndex, this.myTasksPageSize).subscribe({
      next: (page) => {
        this.myTasksDataSource.data = page.content;
        this.myTasksTotal = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load my tasks', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onInboxPageChange(event: PageEvent): void {
    this.inboxPageIndex = event.pageIndex;
    this.inboxPageSize = event.pageSize;
    this.loadInbox();
  }

  onMyTasksPageChange(event: PageEvent): void {
    this.myTasksPageIndex = event.pageIndex;
    this.myTasksPageSize = event.pageSize;
    this.loadMyTasks();
  }

  claimTask(task: TaskResponse): void {
    this.taskService.claimTask(task.taskId).subscribe({
      next: () => {
        this.snackBar.open(`Claimed: ${task.taskName}`, 'Close', { duration: 2000 });
        this.router.navigate(['/tasks', task.taskId]);
      },
      error: () => {
        this.snackBar.open('Failed to claim task — someone else may have claimed it', 'Close', { duration: 3000 });
        this.loadInbox();
      }
    });
  }

  releaseTask(task: TaskResponse): void {
    this.taskService.unclaimTask(task.taskId).subscribe({
      next: () => {
        this.snackBar.open(`Released: ${task.taskName}`, 'Close', { duration: 2000 });
        this.loadMyTasks();
        this.loadInbox();
      },
      error: () => {
        this.snackBar.open('Failed to release task', 'Close', { duration: 3000 });
      }
    });
  }

  viewTask(task: TaskResponse): void {
    this.router.navigate(['/tasks', task.taskId]);
  }

  getTaskLabel(key: string): string {
    return getTaskLabel(key);
  }

  formatLoanType(type: string): string {
    return formatLoanType(type);
  }

  formatCurrency(amount: string | number): string {
    return formatCurrency(amount);
  }

  getRiskColor(risk: string | null): string {
    return risk ? (RISK_COLORS[risk] ?? '') : '';
  }

  getCibilClass(score: number | null): string {
    if (!score) return '';
    if (score >= 750) return 'cibil-good';
    if (score >= 650) return 'cibil-fair';
    return 'cibil-poor';
  }
}
