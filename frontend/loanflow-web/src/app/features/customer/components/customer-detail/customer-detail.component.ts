import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatListModule } from '@angular/material/list';
import { CustomerService } from '../../services/customer.service';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatListModule
  ],
  templateUrl: './customer-detail.component.html',
  styleUrl: './customer-detail.component.scss'
})
export class CustomerDetailComponent implements OnInit {
  customer?: Customer;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private customerService: CustomerService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    this.loadCustomer(id);
  }

  loadCustomer(id: string): void {
    this.customerService.getById(id).subscribe({
      next: (response) => {
        this.customer = response.data;
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Failed to load customer', 'Close', { duration: 3000 });
        this.loading = false;
        console.error('Error:', error);
      }
    });
  }

  verifyKyc(type: 'aadhaar' | 'pan'): void {
    if (!this.customer) return;

    const request$ = type === 'aadhaar'
      ? this.customerService.verifyAadhaar(this.customer.id!)
      : this.customerService.verifyPan(this.customer.id!);

    request$.subscribe({
      next: (response) => {
        this.customer = response.data;
        this.snackBar.open(`${type.toUpperCase()} verified successfully`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Verification failed', 'Close', { duration: 3000 });
      }
    });
  }

  getStatusColor(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'primary';
      case 'VERIFIED': return 'primary';
      case 'PARTIAL': return 'accent';
      case 'INACTIVE':
      case 'REJECTED': return 'warn';
      default: return 'accent';
    }
  }
}
