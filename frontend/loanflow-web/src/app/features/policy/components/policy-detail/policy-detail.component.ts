import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatListModule } from '@angular/material/list';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PolicyService } from '../../services/policy.service';
import { PolicyResponse } from '../../models/policy.model';

@Component({
  selector: 'app-policy-detail',
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
    MatListModule,
    MatExpansionModule,
    MatTooltipModule
  ],
  templateUrl: './policy-detail.component.html',
  styleUrl: './policy-detail.component.scss'
})
export class PolicyDetailComponent implements OnInit {
  policy?: PolicyResponse;
  loading = false;

  constructor(
    private policyService: PolicyService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadPolicy(id);
    }
  }

  loadPolicy(id: string): void {
    this.loading = true;
    this.policyService.getById(id).subscribe({
      next: (response) => {
        this.policy = response.data;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load policy', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/policies']);
      }
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'primary';
      case 'DRAFT': return 'accent';
      case 'INACTIVE': return 'warn';
      default: return '';
    }
  }

  getCategoryLabel(category: string): string {
    return category?.replace(/_/g, ' ') || '';
  }

  getLoanTypeLabel(loanType: string): string {
    return loanType?.replace(/_/g, ' ') || '';
  }

  getOperatorLabel(operator: string): string {
    return operator?.replace(/_/g, ' ').toLowerCase() || '';
  }

  getActionTypeLabel(type: string): string {
    return type?.replace(/_/g, ' ') || '';
  }

  formatDate(date?: string): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  onActivate(): void {
    if (!this.policy) return;
    this.policyService.activate(this.policy.id).subscribe({
      next: (response) => {
        this.policy = response.data;
        this.snackBar.open('Policy activated', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to activate', 'Close', { duration: 3000 });
      }
    });
  }

  onDeactivate(): void {
    if (!this.policy) return;
    this.policyService.deactivate(this.policy.id).subscribe({
      next: (response) => {
        this.policy = response.data;
        this.snackBar.open('Policy deactivated', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to deactivate', 'Close', { duration: 3000 });
      }
    });
  }

  onCreateNewVersion(): void {
    if (!this.policy) return;
    this.policyService.createNewVersion(this.policy.id).subscribe({
      next: (response) => {
        this.snackBar.open('New version created (v' + response.data.versionNumber + ')', 'Close', { duration: 3000 });
        this.router.navigate(['/policies', response.data.id]);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to create version', 'Close', { duration: 3000 });
      }
    });
  }

  onDelete(): void {
    if (!this.policy) return;
    if (confirm('Are you sure you want to delete this policy?')) {
      this.policyService.delete(this.policy.id).subscribe({
        next: () => {
          this.snackBar.open('Policy deleted', 'Close', { duration: 3000 });
          this.router.navigate(['/policies']);
        },
        error: (err) => {
          this.snackBar.open(err.error?.message || 'Failed to delete', 'Close', { duration: 3000 });
        }
      });
    }
  }

  get canEdit(): boolean {
    return this.policy?.status === 'DRAFT' || this.policy?.status === 'INACTIVE';
  }

  get canActivate(): boolean {
    return this.policy?.status === 'DRAFT' || this.policy?.status === 'INACTIVE';
  }

  get canDeactivate(): boolean {
    return this.policy?.status === 'ACTIVE';
  }

  get canDelete(): boolean {
    return this.policy?.status === 'DRAFT';
  }

  get canCreateVersion(): boolean {
    return this.policy?.status === 'ACTIVE' || this.policy?.status === 'INACTIVE';
  }
}
