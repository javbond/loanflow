import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatExpansionModule } from '@angular/material/expansion';
import { PolicyService } from '../../services/policy.service';
import {
  PolicyRequest,
  PolicyResponse,
  POLICY_CATEGORIES,
  LOAN_TYPES,
  CONDITION_OPERATORS,
  ACTION_TYPES
} from '../../models/policy.model';

@Component({
  selector: 'app-policy-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSlideToggleModule,
    MatExpansionModule
  ],
  templateUrl: './policy-form.component.html',
  styleUrl: './policy-form.component.scss'
})
export class PolicyFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  policyId?: string;
  loading = false;
  submitting = false;

  categories = POLICY_CATEGORIES;
  loanTypes = LOAN_TYPES;
  conditionOperators = CONDITION_OPERATORS;
  actionTypes = ACTION_TYPES;

  constructor(
    private fb: FormBuilder,
    private policyService: PolicyService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.policyId = this.route.snapshot.params['id'];

    if (this.policyId && this.policyId !== 'new') {
      this.isEditMode = true;
      this.loadPolicy();
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: [''],
      category: ['', Validators.required],
      loanType: ['', Validators.required],
      priority: [0],
      tags: [''],
      rules: this.fb.array([])
    });
  }

  get rules(): FormArray {
    return this.form.get('rules') as FormArray;
  }

  createRuleGroup(): FormGroup {
    return this.fb.group({
      name: ['', Validators.required],
      description: [''],
      logicalOperator: ['AND'],
      priority: [0],
      enabled: [true],
      conditions: this.fb.array([this.createConditionGroup()]),
      actions: this.fb.array([this.createActionGroup()])
    });
  }

  createConditionGroup(): FormGroup {
    return this.fb.group({
      field: ['', Validators.required],
      operator: ['EQUALS', Validators.required],
      value: [''],
      minValue: [''],
      maxValue: ['']
    });
  }

  createActionGroup(): FormGroup {
    return this.fb.group({
      type: ['APPROVE', Validators.required],
      description: ['']
    });
  }

  addRule(): void {
    this.rules.push(this.createRuleGroup());
  }

  removeRule(index: number): void {
    this.rules.removeAt(index);
  }

  getConditions(ruleIndex: number): FormArray {
    return this.rules.at(ruleIndex).get('conditions') as FormArray;
  }

  getActions(ruleIndex: number): FormArray {
    return this.rules.at(ruleIndex).get('actions') as FormArray;
  }

  addCondition(ruleIndex: number): void {
    this.getConditions(ruleIndex).push(this.createConditionGroup());
  }

  removeCondition(ruleIndex: number, conditionIndex: number): void {
    this.getConditions(ruleIndex).removeAt(conditionIndex);
  }

  addAction(ruleIndex: number): void {
    this.getActions(ruleIndex).push(this.createActionGroup());
  }

  removeAction(ruleIndex: number, actionIndex: number): void {
    this.getActions(ruleIndex).removeAt(actionIndex);
  }

  loadPolicy(): void {
    this.loading = true;
    this.policyService.getById(this.policyId!).subscribe({
      next: (response) => {
        const policy = response.data;
        this.form.patchValue({
          name: policy.name,
          description: policy.description,
          category: policy.category,
          loanType: policy.loanType,
          priority: policy.priority,
          tags: policy.tags?.join(', ') || ''
        });

        // Clear existing rules and rebuild from response
        this.rules.clear();
        if (policy.rules) {
          policy.rules.forEach(rule => {
            const ruleGroup = this.fb.group({
              name: [rule.name, Validators.required],
              description: [rule.description || ''],
              logicalOperator: [rule.logicalOperator || 'AND'],
              priority: [rule.priority || 0],
              enabled: [rule.enabled !== false],
              conditions: this.fb.array([]),
              actions: this.fb.array([])
            });

            const conditionsArray = ruleGroup.get('conditions') as FormArray;
            (rule.conditions || []).forEach(c => {
              conditionsArray.push(this.fb.group({
                field: [c.field, Validators.required],
                operator: [c.operator, Validators.required],
                value: [c.value || ''],
                minValue: [c.minValue || ''],
                maxValue: [c.maxValue || '']
              }));
            });

            const actionsArray = ruleGroup.get('actions') as FormArray;
            (rule.actions || []).forEach(a => {
              actionsArray.push(this.fb.group({
                type: [a.type, Validators.required],
                description: [a.description || '']
              }));
            });

            this.rules.push(ruleGroup);
          });
        }

        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load policy', 'Close', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/policies']);
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.submitting = true;
    const formValue = this.form.value;

    const request: PolicyRequest = {
      name: formValue.name,
      description: formValue.description,
      category: formValue.category,
      loanType: formValue.loanType,
      priority: formValue.priority,
      tags: formValue.tags ? formValue.tags.split(',').map((t: string) => t.trim()).filter((t: string) => t) : [],
      rules: formValue.rules.map((rule: any) => ({
        name: rule.name,
        description: rule.description,
        logicalOperator: rule.logicalOperator,
        priority: rule.priority,
        enabled: rule.enabled,
        conditions: rule.conditions.map((c: any) => ({
          field: c.field,
          operator: c.operator,
          value: c.value || undefined,
          minValue: c.minValue || undefined,
          maxValue: c.maxValue || undefined
        })),
        actions: rule.actions.map((a: any) => ({
          type: a.type,
          description: a.description || undefined,
          parameters: {}
        }))
      }))
    };

    const request$ = this.isEditMode
      ? this.policyService.update(this.policyId!, request)
      : this.policyService.create(request);

    request$.subscribe({
      next: (response) => {
        this.snackBar.open(
          this.isEditMode ? 'Policy updated successfully' : 'Policy created successfully',
          'Close',
          { duration: 3000 }
        );
        this.router.navigate(['/policies', response.data.id]);
      },
      error: (error) => {
        this.snackBar.open(error.error?.message || 'Operation failed', 'Close', { duration: 5000 });
        this.submitting = false;
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup | FormArray): void {
    Object.values(formGroup.controls).forEach(control => {
      if (control instanceof FormGroup || control instanceof FormArray) {
        this.markFormGroupTouched(control);
      } else {
        control.markAsTouched();
      }
    });
  }

  getErrorMessage(field: string): string {
    const control = this.form.get(field);
    if (control?.hasError('required')) return 'This field is required';
    if (control?.hasError('minlength')) return 'Minimum 3 characters required';
    return '';
  }
}
