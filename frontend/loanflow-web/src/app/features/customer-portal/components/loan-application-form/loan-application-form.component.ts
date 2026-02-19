import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatStepperModule } from '@angular/material/stepper';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { LoanService, CustomerLoanApplicationRequest } from '../../../loan/services/loan.service';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { LOAN_TYPES, EMPLOYMENT_TYPES, LoanType } from '../../../loan/models/loan.model';

/**
 * Customer Loan Application Form Component
 * Multi-step wizard for customers to apply for loans
 * Issue: #26 [US-024] Customer Loan Application Form
 */
@Component({
  selector: 'app-customer-loan-application-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatStepperModule,
    MatDividerModule,
    MatProgressBarModule
  ],
  template: `
    <div class="application-container">
      <mat-card class="application-card">
        <mat-card-header>
          <mat-icon mat-card-avatar class="header-icon">account_balance</mat-icon>
          <mat-card-title>Apply for a Loan</mat-card-title>
          <mat-card-subtitle>Complete all steps to submit your application</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <mat-stepper #stepper linear>
            <!-- Step 1: Loan Details -->
            <mat-step [stepControl]="loanDetailsForm" label="Loan Details">
              <form [formGroup]="loanDetailsForm">
                <div class="step-content">
                  <h3>What type of loan do you need?</h3>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Loan Type</mat-label>
                    <mat-select formControlName="loanType" (selectionChange)="onLoanTypeChange()">
                      @for (type of loanTypes; track type.value) {
                        <mat-option [value]="type.value">
                          {{ type.label }} (from {{ type.baseRate }}% p.a.)
                        </mat-option>
                      }
                    </mat-select>
                    <mat-error *ngIf="loanDetailsForm.get('loanType')?.hasError('required')">
                      Please select a loan type
                    </mat-error>
                  </mat-form-field>

                  <div class="row">
                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>Loan Amount (₹)</mat-label>
                      <input matInput type="number" formControlName="requestedAmount"
                             placeholder="Enter amount" (input)="calculateEmi()">
                      <mat-icon matPrefix>currency_rupee</mat-icon>
                      <mat-hint>Min ₹10,000 - Max ₹5 Crore</mat-hint>
                      <mat-error *ngIf="loanDetailsForm.get('requestedAmount')?.hasError('required')">
                        Amount is required
                      </mat-error>
                      <mat-error *ngIf="loanDetailsForm.get('requestedAmount')?.hasError('min')">
                        Minimum amount is ₹10,000
                      </mat-error>
                      <mat-error *ngIf="loanDetailsForm.get('requestedAmount')?.hasError('max')">
                        Maximum amount is ₹5 Crore
                      </mat-error>
                    </mat-form-field>

                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>Tenure (Months)</mat-label>
                      <input matInput type="number" formControlName="tenureMonths"
                             placeholder="e.g., 60" (input)="calculateEmi()">
                      <mat-hint>{{ selectedLoanType ? 'Max ' + selectedLoanType.maxTenureYears * 12 + ' months' : '6 - 360 months' }}</mat-hint>
                      <mat-error *ngIf="loanDetailsForm.get('tenureMonths')?.hasError('required')">
                        Tenure is required
                      </mat-error>
                      <mat-error *ngIf="loanDetailsForm.get('tenureMonths')?.hasError('min')">
                        Minimum tenure is 6 months
                      </mat-error>
                    </mat-form-field>
                  </div>

                  <!-- EMI Calculator -->
                  @if (calculatedEmi > 0) {
                    <div class="emi-preview">
                      <mat-icon>calculate</mat-icon>
                      <div class="emi-info">
                        <span class="emi-label">Estimated Monthly EMI</span>
                        <span class="emi-value">{{ formatCurrency(calculatedEmi) }}</span>
                        <span class="emi-note">*Based on {{ selectedLoanType?.baseRate || 10 }}% interest rate</span>
                      </div>
                    </div>
                  }

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Purpose of Loan</mat-label>
                    <textarea matInput formControlName="purpose" rows="3"
                              placeholder="Briefly describe why you need this loan"></textarea>
                    <mat-hint>Optional - helps us process your application faster</mat-hint>
                  </mat-form-field>
                </div>

                <div class="step-actions">
                  <button mat-button routerLink="/my-portal">
                    <mat-icon>arrow_back</mat-icon> Cancel
                  </button>
                  <button mat-raised-button color="primary" matStepperNext
                          [disabled]="loanDetailsForm.invalid">
                    Next <mat-icon>arrow_forward</mat-icon>
                  </button>
                </div>
              </form>
            </mat-step>

            <!-- Step 2: Personal Information -->
            <mat-step [stepControl]="personalInfoForm" label="Personal Information">
              <form [formGroup]="personalInfoForm">
                <div class="step-content">
                  <h3>Tell us about yourself</h3>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Full Name (as per PAN)</mat-label>
                    <input matInput formControlName="fullName" placeholder="Enter your full name">
                    <mat-icon matPrefix>person</mat-icon>
                    <mat-error *ngIf="personalInfoForm.get('fullName')?.hasError('required')">
                      Full name is required
                    </mat-error>
                  </mat-form-field>

                  <div class="row">
                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>PAN Number</mat-label>
                      <input matInput formControlName="pan" placeholder="ABCDE1234F"
                             style="text-transform: uppercase">
                      <mat-icon matPrefix>badge</mat-icon>
                      <mat-error *ngIf="personalInfoForm.get('pan')?.hasError('required')">
                        PAN is required
                      </mat-error>
                      <mat-error *ngIf="personalInfoForm.get('pan')?.hasError('pattern')">
                        Invalid PAN format (e.g., ABCDE1234F)
                      </mat-error>
                    </mat-form-field>

                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>Aadhaar Number (Optional)</mat-label>
                      <input matInput formControlName="aadhaar" placeholder="1234 5678 9012"
                             maxlength="12">
                      <mat-icon matPrefix>fingerprint</mat-icon>
                      <mat-error *ngIf="personalInfoForm.get('aadhaar')?.hasError('pattern')">
                        Aadhaar must be 12 digits
                      </mat-error>
                    </mat-form-field>
                  </div>

                  <div class="row">
                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>Mobile Number</mat-label>
                      <input matInput formControlName="phone" placeholder="9876543210"
                             maxlength="10">
                      <mat-icon matPrefix>phone</mat-icon>
                      <mat-error *ngIf="personalInfoForm.get('phone')?.hasError('required')">
                        Mobile number is required
                      </mat-error>
                      <mat-error *ngIf="personalInfoForm.get('phone')?.hasError('pattern')">
                        Invalid mobile number (10 digits starting with 6-9)
                      </mat-error>
                    </mat-form-field>

                    <mat-form-field appearance="outline" class="half-width">
                      <mat-label>Email Address</mat-label>
                      <input matInput formControlName="email" placeholder="you@example.com">
                      <mat-icon matPrefix>email</mat-icon>
                      <mat-error *ngIf="personalInfoForm.get('email')?.hasError('required')">
                        Email is required
                      </mat-error>
                      <mat-error *ngIf="personalInfoForm.get('email')?.hasError('email')">
                        Invalid email address
                      </mat-error>
                    </mat-form-field>
                  </div>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Current Address</mat-label>
                    <textarea matInput formControlName="address" rows="2"
                              placeholder="House/Flat No., Street, City, State, PIN"></textarea>
                    <mat-icon matPrefix>home</mat-icon>
                  </mat-form-field>
                </div>

                <div class="step-actions">
                  <button mat-button matStepperPrevious>
                    <mat-icon>arrow_back</mat-icon> Back
                  </button>
                  <button mat-raised-button color="primary" matStepperNext
                          [disabled]="personalInfoForm.invalid">
                    Next <mat-icon>arrow_forward</mat-icon>
                  </button>
                </div>
              </form>
            </mat-step>

            <!-- Step 3: Employment Details -->
            <mat-step [stepControl]="employmentForm" label="Employment">
              <form [formGroup]="employmentForm">
                <div class="step-content">
                  <h3>Your Employment Information</h3>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Employment Type</mat-label>
                    <mat-select formControlName="employmentType">
                      @for (type of employmentTypes; track type.value) {
                        <mat-option [value]="type.value">{{ type.label }}</mat-option>
                      }
                    </mat-select>
                    <mat-error *ngIf="employmentForm.get('employmentType')?.hasError('required')">
                      Please select employment type
                    </mat-error>
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width"
                                  *ngIf="employmentForm.get('employmentType')?.value === 'SALARIED'">
                    <mat-label>Employer / Company Name</mat-label>
                    <input matInput formControlName="employerName"
                           placeholder="Enter your employer's name">
                    <mat-icon matPrefix>business</mat-icon>
                  </mat-form-field>

                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Monthly Income (₹)</mat-label>
                    <input matInput type="number" formControlName="monthlyIncome"
                           placeholder="Enter your monthly income">
                    <mat-icon matPrefix>currency_rupee</mat-icon>
                    <mat-hint>Net take-home salary or business income</mat-hint>
                    <mat-error *ngIf="employmentForm.get('monthlyIncome')?.hasError('required')">
                      Monthly income is required
                    </mat-error>
                    <mat-error *ngIf="employmentForm.get('monthlyIncome')?.hasError('min')">
                      Minimum income is ₹10,000
                    </mat-error>
                  </mat-form-field>
                </div>

                <div class="step-actions">
                  <button mat-button matStepperPrevious>
                    <mat-icon>arrow_back</mat-icon> Back
                  </button>
                  <button mat-raised-button color="primary" matStepperNext
                          [disabled]="employmentForm.invalid">
                    Next <mat-icon>arrow_forward</mat-icon>
                  </button>
                </div>
              </form>
            </mat-step>

            <!-- Step 4: Review & Submit -->
            <mat-step label="Review & Submit">
              <div class="step-content">
                <h3>Review Your Application</h3>
                <p class="review-note">Please verify all information before submitting.</p>

                <div class="review-section">
                  <h4><mat-icon>account_balance</mat-icon> Loan Details</h4>
                  <div class="review-grid">
                    <div class="review-item">
                      <span class="label">Loan Type</span>
                      <span class="value">{{ getLoanTypeLabel(loanDetailsForm.get('loanType')?.value) }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Amount</span>
                      <span class="value">{{ formatCurrency(loanDetailsForm.get('requestedAmount')?.value) }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Tenure</span>
                      <span class="value">{{ loanDetailsForm.get('tenureMonths')?.value }} months</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Est. EMI</span>
                      <span class="value highlight">{{ formatCurrency(calculatedEmi) }}/month</span>
                    </div>
                  </div>
                </div>

                <mat-divider></mat-divider>

                <div class="review-section">
                  <h4><mat-icon>person</mat-icon> Personal Information</h4>
                  <div class="review-grid">
                    <div class="review-item">
                      <span class="label">Name</span>
                      <span class="value">{{ personalInfoForm.get('fullName')?.value }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">PAN</span>
                      <span class="value">{{ personalInfoForm.get('pan')?.value | uppercase }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Mobile</span>
                      <span class="value">{{ personalInfoForm.get('phone')?.value }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Email</span>
                      <span class="value">{{ personalInfoForm.get('email')?.value }}</span>
                    </div>
                  </div>
                </div>

                <mat-divider></mat-divider>

                <div class="review-section">
                  <h4><mat-icon>work</mat-icon> Employment Details</h4>
                  <div class="review-grid">
                    <div class="review-item">
                      <span class="label">Type</span>
                      <span class="value">{{ getEmploymentTypeLabel(employmentForm.get('employmentType')?.value) }}</span>
                    </div>
                    <div class="review-item">
                      <span class="label">Monthly Income</span>
                      <span class="value">{{ formatCurrency(employmentForm.get('monthlyIncome')?.value) }}</span>
                    </div>
                    @if (employmentForm.get('employerName')?.value) {
                      <div class="review-item">
                        <span class="label">Employer</span>
                        <span class="value">{{ employmentForm.get('employerName')?.value }}</span>
                      </div>
                    }
                  </div>
                </div>

                <div class="consent-section">
                  <p><mat-icon>info</mat-icon> By submitting this application, you authorize LoanFlow to verify your information and perform credit checks.</p>
                </div>
              </div>

              <div class="step-actions">
                <button mat-button matStepperPrevious [disabled]="submitting">
                  <mat-icon>arrow_back</mat-icon> Back
                </button>
                <button mat-raised-button color="primary"
                        (click)="onSubmit()"
                        [disabled]="submitting || !isFormValid()">
                  @if (submitting) {
                    <mat-progress-spinner diameter="20" mode="indeterminate"></mat-progress-spinner>
                    Submitting...
                  } @else {
                    <mat-icon>send</mat-icon> Submit Application
                  }
                </button>
              </div>
            </mat-step>
          </mat-stepper>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .application-container {
      padding: 24px;
      max-width: 900px;
      margin: 0 auto;
    }

    .application-card {
      padding: 16px;
    }

    .header-icon {
      background: linear-gradient(135deg, #1976d2, #42a5f5);
      color: white;
      border-radius: 50%;
      padding: 8px;
      font-size: 28px;
      width: 44px;
      height: 44px;
    }

    .step-content {
      padding: 24px 8px;
      min-height: 300px;
    }

    .step-content h3 {
      margin: 0 0 24px;
      color: #1976d2;
      font-weight: 500;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .half-width {
      width: calc(50% - 8px);
    }

    .row {
      display: flex;
      gap: 16px;
      margin-bottom: 8px;
    }

    .emi-preview {
      display: flex;
      align-items: center;
      gap: 16px;
      background: linear-gradient(135deg, #e3f2fd, #bbdefb);
      padding: 16px 24px;
      border-radius: 12px;
      margin: 16px 0 24px;
    }

    .emi-preview mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: #1976d2;
    }

    .emi-info {
      display: flex;
      flex-direction: column;
    }

    .emi-label {
      font-size: 0.875rem;
      color: #666;
    }

    .emi-value {
      font-size: 1.5rem;
      font-weight: 600;
      color: #1976d2;
    }

    .emi-note {
      font-size: 0.75rem;
      color: #888;
    }

    .step-actions {
      display: flex;
      justify-content: space-between;
      padding: 16px 8px;
      border-top: 1px solid #eee;
      margin-top: 24px;
    }

    .review-note {
      color: #666;
      margin-bottom: 24px;
    }

    .review-section {
      padding: 16px 0;
    }

    .review-section h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #333;
      margin: 0 0 16px;
      font-weight: 500;
    }

    .review-section h4 mat-icon {
      color: #1976d2;
    }

    .review-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px 24px;
    }

    .review-item {
      display: flex;
      flex-direction: column;
    }

    .review-item .label {
      font-size: 0.75rem;
      color: #888;
      text-transform: uppercase;
    }

    .review-item .value {
      font-size: 1rem;
      color: #333;
      font-weight: 500;
    }

    .review-item .value.highlight {
      color: #1976d2;
      font-weight: 600;
    }

    mat-divider {
      margin: 8px 0;
    }

    .consent-section {
      background: #fff8e1;
      padding: 16px;
      border-radius: 8px;
      margin-top: 24px;
    }

    .consent-section p {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin: 0;
      color: #f57c00;
      font-size: 0.875rem;
    }

    .consent-section mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    @media (max-width: 600px) {
      .row {
        flex-direction: column;
      }

      .half-width {
        width: 100%;
      }

      .review-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CustomerLoanApplicationFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private loanService = inject(LoanService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  loanDetailsForm!: FormGroup;
  personalInfoForm!: FormGroup;
  employmentForm!: FormGroup;

  loanTypes = LOAN_TYPES;
  employmentTypes = EMPLOYMENT_TYPES;
  selectedLoanType: typeof LOAN_TYPES[0] | null = null;
  calculatedEmi = 0;
  submitting = false;

  ngOnInit(): void {
    this.initForms();
    this.prefillUserInfo();
  }

  initForms(): void {
    // Loan details form
    this.loanDetailsForm = this.fb.group({
      loanType: ['', Validators.required],
      requestedAmount: ['', [Validators.required, Validators.min(10000), Validators.max(50000000)]],
      tenureMonths: ['', [Validators.required, Validators.min(6), Validators.max(360)]],
      purpose: ['', Validators.maxLength(500)]
    });

    // Personal information form
    this.personalInfoForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      pan: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)]],
      aadhaar: ['', Validators.pattern(/^[0-9]{12}$/)],
      phone: ['', [Validators.required, Validators.pattern(/^[6-9][0-9]{9}$/)]],
      email: ['', [Validators.required, Validators.email]],
      address: ['', Validators.maxLength(500)]
    });

    // Employment form
    this.employmentForm = this.fb.group({
      employmentType: ['SALARIED', Validators.required],
      employerName: [''],
      monthlyIncome: ['', [Validators.required, Validators.min(10000)]]
    });
  }

  prefillUserInfo(): void {
    const user = this.authService.getCurrentUserSync();
    if (user) {
      this.personalInfoForm.patchValue({
        fullName: user.fullName || '',
        email: user.email || ''
      });
    }
  }

  onLoanTypeChange(): void {
    const type = this.loanDetailsForm.get('loanType')?.value;
    this.selectedLoanType = this.loanTypes.find(t => t.value === type) || null;

    if (this.selectedLoanType) {
      const maxMonths = this.selectedLoanType.maxTenureYears * 12;
      this.loanDetailsForm.get('tenureMonths')?.setValidators([
        Validators.required,
        Validators.min(6),
        Validators.max(maxMonths)
      ]);
      this.loanDetailsForm.get('tenureMonths')?.updateValueAndValidity();
    }
    this.calculateEmi();
  }

  calculateEmi(): void {
    const amount = this.loanDetailsForm.get('requestedAmount')?.value;
    const tenure = this.loanDetailsForm.get('tenureMonths')?.value;
    const rate = this.selectedLoanType?.baseRate || 10;

    if (amount && tenure && rate) {
      this.calculatedEmi = this.loanService.calculateEmi(amount, rate, tenure);
    } else {
      this.calculatedEmi = 0;
    }
  }

  isFormValid(): boolean {
    return this.loanDetailsForm.valid &&
           this.personalInfoForm.valid &&
           this.employmentForm.valid;
  }

  getLoanTypeLabel(value: string): string {
    return this.loanTypes.find(t => t.value === value)?.label || value;
  }

  getEmploymentTypeLabel(value: string): string {
    return this.employmentTypes.find(t => t.value === value)?.label || value;
  }

  formatCurrency(amount: number): string {
    if (!amount) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  onSubmit(): void {
    if (!this.isFormValid()) {
      this.snackBar.open('Please complete all required fields', 'Close', { duration: 3000 });
      return;
    }

    this.submitting = true;

    const request: CustomerLoanApplicationRequest = {
      loanType: this.loanDetailsForm.value.loanType,
      requestedAmount: this.loanDetailsForm.value.requestedAmount,
      tenureMonths: this.loanDetailsForm.value.tenureMonths,
      purpose: this.loanDetailsForm.value.purpose,
      fullName: this.personalInfoForm.value.fullName,
      pan: this.personalInfoForm.value.pan.toUpperCase(),
      aadhaar: this.personalInfoForm.value.aadhaar || undefined,
      phone: this.personalInfoForm.value.phone,
      email: this.personalInfoForm.value.email,
      address: this.personalInfoForm.value.address || undefined,
      employmentType: this.employmentForm.value.employmentType,
      employerName: this.employmentForm.value.employerName || undefined,
      monthlyIncome: this.employmentForm.value.monthlyIncome
    };

    this.loanService.applyForLoan(request).subscribe({
      next: (response) => {
        this.snackBar.open(
          `Application submitted successfully! Your application number: ${response.data.applicationNumber}`,
          'Close',
          { duration: 5000 }
        );
        this.router.navigate(['/my-portal']);
      },
      error: (error) => {
        this.snackBar.open(
          error.error?.message || 'Failed to submit application. Please try again.',
          'Close',
          { duration: 3000 }
        );
        this.submitting = false;
      }
    });
  }
}
