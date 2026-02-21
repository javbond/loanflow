import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  decision: string;
  applicationNumber: string;
  approvedAmount?: number;
  interestRate?: number;
  rejectionReason?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon [class]="getIconClass()">{{ getIcon() }}</mat-icon>
      Confirm {{ data.decision | titlecase }}
    </h2>
    <mat-dialog-content>
      <p>Are you sure you want to <strong>{{ data.decision | lowercase }}</strong> application <strong>{{ data.applicationNumber }}</strong>?</p>

      @if (data.decision === 'APPROVED') {
        <div class="detail-row">
          <span class="detail-label">Approved Amount:</span>
          <span class="detail-value">{{ formatCurrency(data.approvedAmount) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Interest Rate:</span>
          <span class="detail-value">{{ data.interestRate }}% p.a.</span>
        </div>
      }

      @if (data.decision === 'REJECTED' && data.rejectionReason) {
        <div class="detail-row">
          <span class="detail-label">Reason:</span>
          <span class="detail-value">{{ data.rejectionReason }}</span>
        </div>
      }

      <p class="warning">This action cannot be undone.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button [color]="getButtonColor()" [mat-dialog-close]="true">
        <mat-icon>{{ getIcon() }}</mat-icon>
        Confirm {{ data.decision | titlecase }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .icon-approve { color: #388e3c; }
    .icon-reject { color: #d32f2f; }
    .icon-refer { color: #f57c00; }

    .detail-row {
      display: flex;
      gap: 8px;
      margin: 8px 0;
      padding: 8px 12px;
      background: rgba(0, 0, 0, 0.04);
      border-radius: 4px;
    }

    .detail-label {
      font-weight: 500;
      color: rgba(0, 0, 0, 0.6);
    }

    .detail-value {
      font-weight: 600;
    }

    .warning {
      color: rgba(0, 0, 0, 0.54);
      font-size: 13px;
      font-style: italic;
      margin-top: 16px;
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  getIcon(): string {
    switch (this.data.decision) {
      case 'APPROVED': return 'check_circle';
      case 'REJECTED': return 'cancel';
      case 'REFERRED': return 'supervisor_account';
      default: return 'gavel';
    }
  }

  getIconClass(): string {
    switch (this.data.decision) {
      case 'APPROVED': return 'icon-approve';
      case 'REJECTED': return 'icon-reject';
      case 'REFERRED': return 'icon-refer';
      default: return '';
    }
  }

  getButtonColor(): string {
    switch (this.data.decision) {
      case 'APPROVED': return 'primary';
      case 'REJECTED': return 'warn';
      case 'REFERRED': return 'accent';
      default: return '';
    }
  }

  formatCurrency(amount?: number): string {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }
}
