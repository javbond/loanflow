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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { CustomerService } from '../../services/customer.service';
import { Customer, CustomerRequest } from '../../models/customer.model';

@Component({
  selector: 'app-customer-form',
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
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './customer-form.component.html',
  styleUrl: './customer-form.component.scss'
})
export class CustomerFormComponent implements OnInit {
  customerForm!: FormGroup;
  isEditMode = false;
  customerId?: string;
  loading = false;
  submitting = false;

  genderOptions = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
    { value: 'OTHER', label: 'Other' }
  ];

  indianStates = [
    'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
    'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka',
    'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram',
    'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu',
    'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
    'Delhi', 'Jammu and Kashmir', 'Ladakh'
  ];

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.customerId = this.route.snapshot.params['id'];

    if (this.customerId && this.customerId !== 'new') {
      this.isEditMode = true;
      this.loadCustomer();
    }
  }

  initForm(): void {
    this.customerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      panNumber: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)]],
      aadhaarNumber: ['', [Validators.required, Validators.pattern(/^\d{12}$/)]],
      currentAddress: this.fb.group({
        addressLine1: ['', [Validators.required, Validators.maxLength(200)]],
        addressLine2: [''],
        landmark: [''],
        city: ['', Validators.required],
        state: ['', Validators.required],
        pinCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
        country: ['IN']
      })
    });
  }

  loadCustomer(): void {
    this.loading = true;
    this.customerService.getById(this.customerId!).subscribe({
      next: (response) => {
        const customer = response.data;
        this.customerForm.patchValue({
          firstName: customer.firstName,
          lastName: customer.lastName,
          email: customer.email,
          mobileNumber: customer.mobileNumber,
          dateOfBirth: customer.dateOfBirth,
          gender: customer.gender,
          // Don't load masked PAN/Aadhaar - leave empty (optional on edit)
          panNumber: '',
          aadhaarNumber: '',
          currentAddress: customer.currentAddress
        });
        // Make PAN/Aadhaar optional in edit mode
        this.customerForm.get('panNumber')?.clearValidators();
        this.customerForm.get('aadhaarNumber')?.clearValidators();
        this.customerForm.get('panNumber')?.updateValueAndValidity();
        this.customerForm.get('aadhaarNumber')?.updateValueAndValidity();
        this.loading = false;
      },
      error: (error) => {
        this.snackBar.open('Failed to load customer', 'Close', { duration: 3000 });
        this.loading = false;
        console.error('Error loading customer:', error);
      }
    });
  }

  onSubmit(): void {
    if (this.customerForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.submitting = true;
    const formValue = this.customerForm.value;

    // Format date
    const customerData: CustomerRequest = {
      ...formValue,
      dateOfBirth: this.formatDate(formValue.dateOfBirth)
    };

    const request$ = this.isEditMode
      ? this.customerService.update(this.customerId!, customerData)
      : this.customerService.create(customerData);

    request$.subscribe({
      next: (response) => {
        this.snackBar.open(
          this.isEditMode ? 'Customer updated successfully' : 'Customer created successfully',
          'Close',
          { duration: 3000 }
        );
        this.router.navigate(['/customers', response.data.id]);
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Operation failed', 'Close', { duration: 5000 });
        this.submitting = false;
        console.error('Error:', error);
      }
    });
  }

  private formatDate(date: Date | string): string {
    if (typeof date === 'string') return date;
    return date.toISOString().split('T')[0];
  }

  private markFormGroupTouched(): void {
    Object.keys(this.customerForm.controls).forEach(key => {
      const control = this.customerForm.get(key);
      control?.markAsTouched();
      if (control instanceof FormGroup) {
        Object.keys(control.controls).forEach(k => control.get(k)?.markAsTouched());
      }
    });
  }

  getErrorMessage(field: string): string {
    const control = this.customerForm.get(field);
    if (control?.hasError('required')) return 'This field is required';
    if (control?.hasError('email')) return 'Invalid email address';
    if (control?.hasError('pattern')) {
      if (field === 'mobileNumber') return 'Enter valid 10-digit Indian mobile (e.g., 9876543210)';
      if (field === 'panNumber') return 'Enter valid PAN (e.g., ABCDE1234F)';
      if (field === 'aadhaarNumber') return 'Enter valid 12-digit Aadhaar';
      if (field.includes('pinCode')) return 'Enter valid 6-digit PIN code';
    }
    if (control?.hasError('minlength')) return 'Too short';
    if (control?.hasError('maxlength')) return 'Too long';
    return '';
  }
}
