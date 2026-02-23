import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { EkycService } from '../../services/ekyc.service';
import {
  EkycData,
  EkycPanelState,
  KycStatusResponse,
  KYC_STATUS_COLORS,
  KYC_STATUS_LABELS
} from '../../models/ekyc.model';

/**
 * Reusable e-KYC verification panel component (US-029).
 * Handles the full OTP-based Aadhaar verification flow.
 */
@Component({
  selector: 'app-ekyc-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
    MatListModule
  ],
  templateUrl: './ekyc-panel.component.html',
  styleUrls: ['./ekyc-panel.component.scss']
})
export class EkycPanelComponent implements OnInit {
  @Input() customerId: string | undefined;

  state: EkycPanelState = 'IDLE';
  kycStatus: KycStatusResponse | null = null;
  aadhaarNumber = '';
  otp = '';
  transactionId = '';
  maskedMobile = '';
  ekycData: EkycData | null = null;
  ckycNumber: string | null = null;
  errorMessage = '';
  loading = false;

  constructor(
    private ekycService: EkycService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    if (this.customerId) {
      this.loadKycStatus();
    }
  }

  loadKycStatus(): void {
    if (!this.customerId) return;
    this.loading = true;

    this.ekycService.getKycStatus(this.customerId).subscribe({
      next: (response) => {
        this.kycStatus = response.data;
        this.loading = false;

        if (this.kycStatus?.status === 'VERIFIED') {
          this.state = 'VERIFIED';
          this.ekycData = this.kycStatus.ekycData || null;
          this.ckycNumber = this.kycStatus.ckycNumber || null;
        } else if (this.kycStatus?.status === 'OTP_SENT') {
          this.state = 'OTP_INPUT';
        }
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load KYC status', 'Close', { duration: 3000 });
      }
    });
  }

  initiateEkyc(): void {
    if (!this.customerId || !this.aadhaarNumber || this.aadhaarNumber.length !== 12) {
      this.snackBar.open('Please enter a valid 12-digit Aadhaar number', 'Close', { duration: 3000 });
      return;
    }

    this.state = 'INITIATING';
    this.errorMessage = '';

    this.ekycService.initiateEkyc(this.customerId, this.aadhaarNumber).subscribe({
      next: (response) => {
        const data = response.data;
        if (data.status === 'OTP_SENT') {
          this.transactionId = data.transactionId;
          this.maskedMobile = data.maskedMobile;
          this.state = 'OTP_INPUT';
          this.snackBar.open('OTP sent to ' + data.maskedMobile, 'Close', { duration: 3000 });
        } else if (data.status === 'ALREADY_VERIFIED') {
          this.state = 'VERIFIED';
          this.loadKycStatus();
          this.snackBar.open('e-KYC already verified', 'Close', { duration: 3000 });
        } else {
          this.state = 'FAILED';
          this.errorMessage = data.message;
        }
      },
      error: (error) => {
        this.state = 'FAILED';
        this.errorMessage = error.error?.message || 'Failed to initiate e-KYC';
        this.snackBar.open(this.errorMessage, 'Close', { duration: 3000 });
      }
    });
  }

  verifyOtp(): void {
    if (!this.customerId || !this.otp || this.otp.length !== 6) {
      this.snackBar.open('Please enter a valid 6-digit OTP', 'Close', { duration: 3000 });
      return;
    }

    this.state = 'VERIFYING';
    this.errorMessage = '';

    this.ekycService.verifyOtp(this.customerId, {
      transactionId: this.transactionId,
      otp: this.otp
    }).subscribe({
      next: (response) => {
        const data = response.data;
        if (data.verified) {
          this.state = 'VERIFIED';
          this.ekycData = data.ekycData || null;
          this.ckycNumber = data.ckycNumber || null;
          this.snackBar.open('e-KYC verification successful!', 'Close', { duration: 3000 });
        } else {
          this.state = 'FAILED';
          this.errorMessage = data.message;
          this.otp = '';
        }
      },
      error: (error) => {
        this.state = 'FAILED';
        this.errorMessage = error.error?.message || 'OTP verification failed';
        this.otp = '';
        this.snackBar.open(this.errorMessage, 'Close', { duration: 3000 });
      }
    });
  }

  retry(): void {
    this.state = 'IDLE';
    this.otp = '';
    this.errorMessage = '';
  }

  getMaskedAadhaar(): string {
    if (!this.aadhaarNumber || this.aadhaarNumber.length !== 12) {
      return this.kycStatus?.maskedAadhaar || '';
    }
    return 'XXXX XXXX ' + this.aadhaarNumber.substring(8);
  }

  getStatusColor(status?: string): string {
    return KYC_STATUS_COLORS[status || ''] || '';
  }

  getStatusLabel(status?: string): string {
    return KYC_STATUS_LABELS[status || ''] || status || 'Unknown';
  }
}
