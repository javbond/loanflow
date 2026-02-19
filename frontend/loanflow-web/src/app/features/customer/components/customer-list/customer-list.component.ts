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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-customer-list',
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
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.scss'
})
export class CustomerListComponent implements OnInit {
  displayedColumns: string[] = ['customerNumber', 'fullName', 'email', 'mobileNumber', 'status', 'kycStatus', 'actions'];
  dataSource = new MatTableDataSource<Customer>([]);
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  loading = false;
  searchQuery = '';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private customerService: CustomerService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loading = true;
    this.customerService.list(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.dataSource.data = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Failed to load customers', 'Close', { duration: 3000 });
        this.loading = false;
        console.error('Error loading customers:', error);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadCustomers();
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.loading = true;
      this.customerService.search(this.searchQuery, 0, this.pageSize).subscribe({
        next: (response) => {
          this.dataSource.data = response.content;
          this.totalElements = response.totalElements;
          this.pageIndex = 0;
          this.loading = false;
        },
        error: () => {
          this.snackBar.open('Search failed', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    } else {
      this.loadCustomers();
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.loadCustomers();
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'primary';
      case 'INACTIVE': return 'warn';
      case 'BLOCKED': return 'warn';
      default: return 'accent';
    }
  }

  getKycStatusColor(status: string): string {
    switch (status) {
      case 'VERIFIED': return 'primary';
      case 'PARTIAL': return 'accent';
      case 'REJECTED': return 'warn';
      default: return 'accent';
    }
  }
}
