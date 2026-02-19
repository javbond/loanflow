import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { LoanService } from '../../services/loan.service';
import {
  LoanApplication,
  LoanApplicationRequest,
  LOAN_TYPES,
  EMPLOYMENT_TYPES,
  PROPERTY_TYPES,
  LoanType
} from '../../models/loan.model';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

interface Customer {
  id: string;
  firstName: string;
  lastName: string;
  mobileNumber: string;
  panNumber: string;
}

@Component({
  selector: 'app-loan-form',
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
    MatAutocompleteModule
  ],
  templateUrl: './loan-form.component.html',
  styleUrl: './loan-form.component.scss'
})
export class LoanFormComponent implements OnInit {
  loanForm!: FormGroup;
  employmentForm!: FormGroup;
  propertyForm!: FormGroup;

  isEditMode = false;
  loanId: string | null = null;
  loading = false;
  submitting = false;

  loanTypes = LOAN_TYPES;
  employmentTypes = EMPLOYMENT_TYPES;
  propertyTypes = PROPERTY_TYPES;

  customers: Customer[] = [];
  filteredCustomers: Customer[] = [];
  selectedCustomer: Customer | null = null;

  calculatedEmi: number = 0;
  selectedLoanType: typeof LOAN_TYPES[0] | null = null;

  constructor(
    private fb: FormBuilder,
    private loanService: LoanService,
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loanId = this.route.snapshot.paramMap.get('id');

    if (this.loanId) {
      this.isEditMode = true;
      this.loadLoan(this.loanId);
    }
  }

  initForms(): void {
    // Main loan form
    this.loanForm = this.fb.group({
      customerId: ['', Validators.required],
      customerSearch: [''],
      loanType: ['', Validators.required],
      requestedAmount: ['', [Validators.required, Validators.min(10000), Validators.max(100000000)]],
      tenureMonths: ['', [Validators.required, Validators.min(6), Validators.max(360)]],
      purpose: ['', [Validators.required, Validators.maxLength(500)]],
      branchCode: ['MUM001']
    });

    // Employment details form
    this.employmentForm = this.fb.group({
      employmentType: ['SALARIED', Validators.required],
      employerName: [''],
      businessName: [''],
      monthlyIncome: ['', [Validators.required, Validators.min(0)]],
      yearsOfExperience: ['', Validators.min(0)]
    });

    // Property details form (for secured loans)
    this.propertyForm = this.fb.group({
      propertyType: [''],
      address: [''],
      city: [''],
      state: [''],
      pinCode: ['', Validators.pattern(/^[1-9][0-9]{5}$/)],
      estimatedValue: ['']
    });

    // Watch for loan type changes to update max tenure
    this.loanForm.get('loanType')?.valueChanges.subscribe((type: LoanType) => {
      this.selectedLoanType = this.loanTypes.find(t => t.value === type) || null;
      if (this.selectedLoanType) {
        const maxMonths = this.selectedLoanType.maxTenureYears * 12;
        this.loanForm.get('tenureMonths')?.setValidators([
          Validators.required,
          Validators.min(6),
          Validators.max(maxMonths)
        ]);
        this.loanForm.get('tenureMonths')?.updateValueAndValidity();
      }
      this.calculateEmi();
    });

    // Watch for amount/tenure changes to calculate EMI
    this.loanForm.get('requestedAmount')?.valueChanges.subscribe(() => this.calculateEmi());
    this.loanForm.get('tenureMonths')?.valueChanges.subscribe(() => this.calculateEmi());

    // Customer search
    this.loanForm.get('customerSearch')?.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(value => {
      if (value && value.length >= 2) {
        this.searchCustomers(value);
      }
    });
  }

  searchCustomers(query: string): void {
    this.http.get<any>(`/api/v1/customers/search?query=${encodeURIComponent(query)}`).pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(response => {
      this.filteredCustomers = response.content || [];
    });
  }

  selectCustomer(customer: Customer): void {
    this.selectedCustomer = customer;
    this.loanForm.patchValue({
      customerId: customer.id,
      customerSearch: `${customer.firstName} ${customer.lastName} (${customer.panNumber})`
    });
  }

  displayCustomer(customer: Customer): string {
    return customer ? `${customer.firstName} ${customer.lastName}` : '';
  }

  calculateEmi(): void {
    const amount = this.loanForm.get('requestedAmount')?.value;
    const tenure = this.loanForm.get('tenureMonths')?.value;
    const rate = this.selectedLoanType?.baseRate || 10;

    if (amount && tenure && rate) {
      this.calculatedEmi = this.loanService.calculateEmi(amount, rate, tenure);
    } else {
      this.calculatedEmi = 0;
    }
  }

  loadLoan(id: string): void {
    this.loading = true;
    this.loanService.getById(id).subscribe({
      next: (response) => {
        const loan = response.data;
        this.loanForm.patchValue({
          customerId: loan.customerId,
          customerSearch: loan.customerName || '',
          loanType: loan.loanType,
          requestedAmount: loan.requestedAmount,
          tenureMonths: loan.tenureMonths,
          purpose: loan.purpose,
          branchCode: loan.branchCode
        });
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load loan application', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/loans']);
      }
    });
  }

  isSecuredLoan(): boolean {
    const type = this.loanForm.get('loanType')?.value;
    return ['HOME_LOAN', 'VEHICLE_LOAN', 'LAP', 'GOLD_LOAN'].includes(type);
  }

  onSubmit(): void {
    if (this.loanForm.invalid || this.employmentForm.invalid) {
      this.markFormsTouched();
      return;
    }

    if (this.isSecuredLoan() && this.propertyForm.invalid) {
      this.propertyForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const request: LoanApplicationRequest = {
      customerId: this.loanForm.value.customerId,
      loanType: this.loanForm.value.loanType,
      requestedAmount: this.loanForm.value.requestedAmount,
      tenureMonths: this.loanForm.value.tenureMonths,
      purpose: this.loanForm.value.purpose,
      branchCode: this.loanForm.value.branchCode,
      employmentDetails: {
        employmentType: this.employmentForm.value.employmentType,
        employerName: this.employmentForm.value.employerName,
        businessName: this.employmentForm.value.businessName,
        monthlyIncome: this.employmentForm.value.monthlyIncome,
        yearsOfExperience: this.employmentForm.value.yearsOfExperience
      }
    };

    if (this.isSecuredLoan()) {
      request.propertyDetails = {
        propertyType: this.propertyForm.value.propertyType,
        address: this.propertyForm.value.address,
        city: this.propertyForm.value.city,
        state: this.propertyForm.value.state,
        pinCode: this.propertyForm.value.pinCode,
        estimatedValue: this.propertyForm.value.estimatedValue
      };
    }

    const operation = this.isEditMode
      ? this.loanService.update(this.loanId!, request)
      : this.loanService.create(request);

    operation.subscribe({
      next: (response) => {
        this.snackBar.open(
          this.isEditMode ? 'Loan application updated' : 'Loan application created',
          'Close',
          { duration: 3000 }
        );
        this.router.navigate(['/loans', response.data.id]);
      },
      error: (error) => {
        this.snackBar.open(
          error.error?.message || 'Failed to save loan application',
          'Close',
          { duration: 3000 }
        );
        this.submitting = false;
      }
    });
  }

  markFormsTouched(): void {
    this.loanForm.markAllAsTouched();
    this.employmentForm.markAllAsTouched();
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }
}
