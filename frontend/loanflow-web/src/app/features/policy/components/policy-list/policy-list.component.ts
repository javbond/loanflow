import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { PolicyService } from '../../services/policy.service';
import { PolicyResponse, POLICY_CATEGORIES } from '../../models/policy.model';

@Component({
  selector: 'app-policy-list',
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
    MatCardModule,
    MatChipsModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './policy-list.component.html',
  styleUrl: './policy-list.component.scss'
})
export class PolicyListComponent implements OnInit {
  displayedColumns: string[] = ['policyCode', 'name', 'category', 'loanType', 'status', 'versionNumber', 'ruleCount', 'actions'];
  dataSource = new MatTableDataSource<PolicyResponse>([]);
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  loading = false;
  searchQuery = '';
  categoryFilter = '';

  categories = POLICY_CATEGORIES;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private policyService: PolicyService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadPolicies();
  }

  loadPolicies(): void {
    this.loading = true;

    const request$ = this.categoryFilter
      ? this.policyService.listByCategory(this.categoryFilter, this.pageIndex, this.pageSize)
      : this.policyService.list(this.pageIndex, this.pageSize);

    request$.subscribe({
      next: (response) => {
        const page = response.data;
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Failed to load policies', 'Close', { duration: 3000 });
        this.loading = false;
        console.error('Error loading policies:', error);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    if (this.searchQuery.trim()) {
      this.onSearch();
    } else {
      this.loadPolicies();
    }
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.loading = true;
      this.policyService.search(this.searchQuery, this.pageIndex, this.pageSize).subscribe({
        next: (response) => {
          const page = response.data;
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.snackBar.open('Search failed', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    } else {
      this.pageIndex = 0;
      this.loadPolicies();
    }
  }

  onCategoryFilter(): void {
    this.pageIndex = 0;
    this.searchQuery = '';
    this.loadPolicies();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.categoryFilter = '';
    this.pageIndex = 0;
    this.loadPolicies();
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'primary';
      case 'DRAFT': return 'accent';
      case 'INACTIVE': return 'warn';
      case 'ARCHIVED': return '';
      default: return '';
    }
  }

  getCategoryLabel(category: string): string {
    return category?.replace(/_/g, ' ') || '';
  }

  getLoanTypeLabel(loanType: string): string {
    return loanType?.replace(/_/g, ' ') || '';
  }

  onActivate(id: string, event: Event): void {
    event.stopPropagation();
    this.policyService.activate(id).subscribe({
      next: () => {
        this.snackBar.open('Policy activated', 'Close', { duration: 3000 });
        this.loadPolicies();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to activate', 'Close', { duration: 3000 });
      }
    });
  }

  onDeactivate(id: string, event: Event): void {
    event.stopPropagation();
    this.policyService.deactivate(id).subscribe({
      next: () => {
        this.snackBar.open('Policy deactivated', 'Close', { duration: 3000 });
        this.loadPolicies();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to deactivate', 'Close', { duration: 3000 });
      }
    });
  }

  onDelete(id: string, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this policy?')) {
      this.policyService.delete(id).subscribe({
        next: () => {
          this.snackBar.open('Policy deleted', 'Close', { duration: 3000 });
          this.loadPolicies();
        },
        error: (err) => {
          this.snackBar.open(err.error?.message || 'Failed to delete', 'Close', { duration: 3000 });
        }
      });
    }
  }
}
