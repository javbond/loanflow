import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoanService } from '../../services/loan.service';
import { LoanApplication, LOAN_STATUSES, LoanStatus } from '../../models/loan.model';

@Component({
  selector: 'app-loan-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSelectModule,
    MatCardModule,
    MatTooltipModule
  ],
  templateUrl: './loan-list.component.html',
  styleUrl: './loan-list.component.scss'
})
export class LoanListComponent implements OnInit {
  displayedColumns: string[] = [
    'applicationNumber',
    'customerName',
    'loanType',
    'requestedAmount',
    'status',
    'createdAt',
    'actions'
  ];

  dataSource = new MatTableDataSource<LoanApplication>();
  loading = true;
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  statusFilter: LoanStatus | '' = '';
  loanStatuses = LOAN_STATUSES;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private loanService: LoanService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;

    const loadFn = this.statusFilter
      ? this.loanService.getByStatus(this.statusFilter, this.pageIndex, this.pageSize)
      : this.loanService.list(this.pageIndex, this.pageSize);

    loadFn.subscribe({
      next: (response) => {
        this.dataSource.data = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Failed to load loan applications', 'Close', { duration: 3000 });
        this.loading = false;
        console.error('Error:', error);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLoans();
  }

  onStatusFilterChange(): void {
    this.pageIndex = 0;
    this.loadLoans();
  }

  getStatusColor(status?: string): string {
    const statusInfo = LOAN_STATUSES.find(s => s.value === status);
    return statusInfo?.color || 'accent';
  }

  getStatusLabel(status?: string): string {
    const statusInfo = LOAN_STATUSES.find(s => s.value === status);
    return statusInfo?.label || status || 'Unknown';
  }

  formatLoanType(type: string): string {
    return type?.replace(/_/g, ' ') || '';
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
}
