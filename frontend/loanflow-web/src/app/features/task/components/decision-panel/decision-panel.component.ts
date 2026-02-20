import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { TaskResponse, CompleteTaskRequest, TaskDecision } from '../../models/task.model';
import { LoanApplication } from '../../../loan/models/loan.model';
import { ConfirmDialogComponent } from './confirm-dialog.component';

@Component({
  selector: 'app-decision-panel',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatDividerModule,
    ConfirmDialogComponent
  ],
  templateUrl: './decision-panel.component.html',
  styleUrls: ['./decision-panel.component.scss']
})
export class DecisionPanelComponent implements OnInit {
  @Input() task!: TaskResponse;
  @Input() loan: LoanApplication | null = null;
  @Input() loading = false;
  @Output() decisionSubmitted = new EventEmitter<CompleteTaskRequest>();

  decisionForm!: FormGroup;
  selectedDecision: TaskDecision | null = null;

  constructor(
    private fb: FormBuilder,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.decisionForm = this.fb.group({
      decision: ['', Validators.required],
      comments: [''],
      approvedAmount: [null],
      interestRate: [null],
      rejectionReason: ['']
    });

    // Pre-fill approval fields from loan data
    if (this.loan) {
      this.decisionForm.patchValue({
        approvedAmount: this.loan.requestedAmount,
        interestRate: this.loan.interestRate || this.getDefaultRate()
      });
    }

    // Listen for decision change to update validators
    this.decisionForm.get('decision')!.valueChanges.subscribe((decision: TaskDecision) => {
      this.selectedDecision = decision;
      this.updateValidators(decision);
    });
  }

  private getDefaultRate(): number {
    // Default interest rates by loan type
    const rates: Record<string, number> = {
      HOME_LOAN: 8.5,
      PERSONAL_LOAN: 12.0,
      VEHICLE_LOAN: 9.5,
      BUSINESS_LOAN: 14.0,
      EDUCATION_LOAN: 8.0,
      GOLD_LOAN: 7.5,
      LAP: 9.0
    };
    return rates[this.task?.loanType] || 10.0;
  }

  private updateValidators(decision: TaskDecision): void {
    const approvedAmount = this.decisionForm.get('approvedAmount')!;
    const interestRate = this.decisionForm.get('interestRate')!;
    const rejectionReason = this.decisionForm.get('rejectionReason')!;

    // Clear all conditional validators
    approvedAmount.clearValidators();
    interestRate.clearValidators();
    rejectionReason.clearValidators();

    if (decision === 'APPROVED') {
      approvedAmount.setValidators([Validators.required, Validators.min(1)]);
      interestRate.setValidators([Validators.required, Validators.min(0.1), Validators.max(30)]);
    } else if (decision === 'REJECTED') {
      rejectionReason.setValidators([Validators.required, Validators.minLength(10)]);
    }
    // REFERRED — only optional comments

    approvedAmount.updateValueAndValidity();
    interestRate.updateValueAndValidity();
    rejectionReason.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.decisionForm.invalid) {
      this.decisionForm.markAllAsTouched();
      return;
    }

    const formValue = this.decisionForm.value;
    const decision = formValue.decision as TaskDecision;

    // Open confirmation dialog
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        decision,
        applicationNumber: this.task.applicationNumber,
        approvedAmount: formValue.approvedAmount,
        interestRate: formValue.interestRate,
        rejectionReason: formValue.rejectionReason
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        const request: CompleteTaskRequest = {
          decision,
          comments: formValue.comments || undefined,
          approvedAmount: decision === 'APPROVED' ? formValue.approvedAmount : undefined,
          interestRate: decision === 'APPROVED' ? formValue.interestRate : undefined,
          rejectionReason: decision === 'REJECTED' ? formValue.rejectionReason : undefined
        };
        this.decisionSubmitted.emit(request);
      }
    });
  }

  getDecisionIcon(): string {
    switch (this.selectedDecision) {
      case 'APPROVED': return 'check_circle';
      case 'REJECTED': return 'cancel';
      case 'REFERRED': return 'supervisor_account';
      default: return 'gavel';
    }
  }

  getDecisionColor(): string {
    switch (this.selectedDecision) {
      case 'APPROVED': return 'primary';
      case 'REJECTED': return 'warn';
      case 'REFERRED': return 'accent';
      default: return '';
    }
  }

  getRiskColor(risk: string): string {
    const colors: Record<string, string> = { LOW: 'primary', MEDIUM: 'accent', HIGH: 'warn' };
    return colors[risk] || '';
  }
}
