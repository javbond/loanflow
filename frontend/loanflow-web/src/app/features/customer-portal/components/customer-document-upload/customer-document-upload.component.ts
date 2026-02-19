import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpEventType } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { Subject, takeUntil } from 'rxjs';

import { DocumentService } from '../../../document/services/document.service';
import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication } from '../../../loan/models/loan.model';
import { AuthService } from '../../../../core/auth/services/auth.service';
import {
  DocumentUploadRequest,
  DOCUMENT_TYPES,
  DOCUMENT_CATEGORIES,
  DocumentTypeInfo,
  MAX_FILE_SIZE_DISPLAY,
  ALLOWED_FILE_EXTENSIONS,
  isFileTypeAllowed,
  isFileSizeAllowed,
  formatFileSize,
  getDocumentTypesByCategory
} from '../../../document/models/document.model';

/**
 * Customer Document Upload Component
 * Simplified upload interface for customers
 * Issue: #27 [US-025] Customer Document Upload
 */
@Component({
  selector: 'app-customer-document-upload',
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
    MatProgressBarModule,
    MatSnackBarModule,
    MatChipsModule,
    MatDividerModule
  ],
  template: `
    <div class="upload-container">
      <mat-card class="upload-card">
        <mat-card-header>
          <mat-icon mat-card-avatar class="header-icon">cloud_upload</mat-icon>
          <mat-card-title>Upload Document</mat-card-title>
          <mat-card-subtitle>Upload supporting documents for your loan application</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="uploadForm" (ngSubmit)="onSubmit()">
            <!-- Select Application -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Select Loan Application</mat-label>
              <mat-select formControlName="applicationId" (selectionChange)="onApplicationChange($event)">
                @for (app of myApplications; track app.id) {
                  <mat-option [value]="app.id">
                    {{ app.applicationNumber }} - {{ getLoanTypeLabel(app.loanType) }}
                    <span class="status-chip" [class]="app.status?.toLowerCase()">{{ app.status }}</span>
                  </mat-option>
                }
              </mat-select>
              @if (myApplications.length === 0 && !loading) {
                <mat-hint class="warn">No applications found. <a routerLink="/my-portal/apply">Apply for a loan</a> first.</mat-hint>
              }
              <mat-error *ngIf="uploadForm.get('applicationId')?.hasError('required')">
                Please select an application
              </mat-error>
            </mat-form-field>

            <!-- Document Category -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Document Category</mat-label>
              <mat-select formControlName="category">
                @for (cat of categories; track cat.value) {
                  <mat-option [value]="cat.value">{{ cat.label }}</mat-option>
                }
              </mat-select>
              <mat-error *ngIf="uploadForm.get('category')?.hasError('required')">
                Please select a category
              </mat-error>
            </mat-form-field>

            <!-- Document Type -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Document Type</mat-label>
              <mat-select formControlName="documentType">
                @for (type of filteredDocumentTypes; track type.value) {
                  <mat-option [value]="type.value">{{ type.label }}</mat-option>
                }
              </mat-select>
              <mat-error *ngIf="uploadForm.get('documentType')?.hasError('required')">
                Please select document type
              </mat-error>
            </mat-form-field>

            <!-- File Drop Zone -->
            <div class="drop-zone"
                 [class.drag-over]="isDragOver"
                 [class.has-file]="selectedFile"
                 (dragover)="onDragOver($event)"
                 (dragleave)="onDragLeave($event)"
                 (drop)="onDrop($event)">

              @if (!selectedFile) {
                <div class="drop-content">
                  <mat-icon class="drop-icon">cloud_upload</mat-icon>
                  <p class="drop-text">Drag & drop your file here</p>
                  <p class="drop-hint">or click to browse</p>
                  <input type="file" #fileInput (change)="onFileSelect($event)"
                         accept=".pdf,.jpg,.jpeg,.png" hidden>
                  <button mat-stroked-button type="button" (click)="fileInput.click()">
                    <mat-icon>folder_open</mat-icon> Browse Files
                  </button>
                  <p class="file-info">
                    Accepted: {{ allowedExtensions.join(', ') }} | Max size: {{ maxFileSizeDisplay }}
                  </p>
                </div>
              } @else {
                <div class="file-preview">
                  <mat-icon class="file-icon">{{ getFileIcon(selectedFile) }}</mat-icon>
                  <div class="file-details">
                    <span class="file-name">{{ selectedFile.name }}</span>
                    <span class="file-size">{{ formatSize(selectedFile.size) }}</span>
                  </div>
                  <button mat-icon-button type="button" (click)="removeFile()" [disabled]="isUploading">
                    <mat-icon>close</mat-icon>
                  </button>
                </div>
              }
            </div>

            <!-- Upload Progress -->
            @if (isUploading) {
              <div class="progress-section">
                <mat-progress-bar mode="determinate" [value]="uploadProgress"></mat-progress-bar>
                <span class="progress-text">Uploading... {{ uploadProgress }}%</span>
              </div>
            }

            <!-- Description -->
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Description (Optional)</mat-label>
              <textarea matInput formControlName="description" rows="2"
                        placeholder="Add any notes about this document"></textarea>
            </mat-form-field>

            <!-- Actions -->
            <div class="form-actions">
              <button mat-button type="button" routerLink="/my-portal">
                <mat-icon>arrow_back</mat-icon> Back
              </button>
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="uploadForm.invalid || !selectedFile || isUploading">
                @if (isUploading) {
                  <mat-icon>hourglass_empty</mat-icon> Uploading...
                } @else {
                  <mat-icon>cloud_upload</mat-icon> Upload Document
                }
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .upload-container {
      padding: 24px;
      max-width: 700px;
      margin: 0 auto;
    }

    .header-icon {
      background: linear-gradient(135deg, #ff9800, #ffb74d);
      color: white;
      border-radius: 50%;
      padding: 8px;
      font-size: 28px;
      width: 44px;
      height: 44px;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .status-chip {
      font-size: 0.7rem;
      padding: 2px 8px;
      border-radius: 12px;
      margin-left: 8px;
      background: #e0e0e0;
    }

    .status-chip.submitted { background: #bbdefb; color: #1565c0; }
    .status-chip.approved { background: #c8e6c9; color: #2e7d32; }
    .status-chip.draft { background: #fff9c4; color: #f57f17; }

    .required-badge {
      font-size: 0.7rem;
      background: #ffcdd2;
      color: #c62828;
      padding: 2px 6px;
      border-radius: 4px;
      margin-left: 8px;
    }

    .drop-zone {
      border: 2px dashed #ccc;
      border-radius: 12px;
      padding: 32px;
      text-align: center;
      transition: all 0.3s ease;
      margin-bottom: 24px;
      cursor: pointer;
    }

    .drop-zone:hover, .drop-zone.drag-over {
      border-color: #1976d2;
      background: #e3f2fd;
    }

    .drop-zone.has-file {
      border-color: #4caf50;
      background: #e8f5e9;
    }

    .drop-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #9e9e9e;
    }

    .drop-text {
      font-size: 1.1rem;
      color: #333;
      margin: 12px 0 4px;
    }

    .drop-hint {
      color: #666;
      font-size: 0.875rem;
      margin: 0 0 16px;
    }

    .file-info {
      font-size: 0.75rem;
      color: #888;
      margin-top: 12px;
    }

    .file-preview {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .file-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #1976d2;
    }

    .file-details {
      flex: 1;
      text-align: left;
    }

    .file-name {
      display: block;
      font-weight: 500;
      color: #333;
    }

    .file-size {
      font-size: 0.875rem;
      color: #666;
    }

    .progress-section {
      margin: 16px 0;
    }

    .progress-text {
      display: block;
      text-align: center;
      margin-top: 8px;
      color: #666;
    }

    .form-actions {
      display: flex;
      justify-content: space-between;
      padding-top: 16px;
      border-top: 1px solid #eee;
    }

    a {
      color: #1976d2;
    }
  `]
})
export class CustomerDocumentUploadComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private documentService = inject(DocumentService);
  private loanService = inject(LoanService);
  private authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  uploadForm!: FormGroup;
  categories = DOCUMENT_CATEGORIES;
  filteredDocumentTypes: DocumentTypeInfo[] = [];

  myApplications: LoanApplication[] = [];
  loading = true;

  selectedFile: File | null = null;
  isDragOver = false;
  uploadProgress = 0;
  isUploading = false;

  maxFileSizeDisplay = MAX_FILE_SIZE_DISPLAY;
  allowedExtensions = ALLOWED_FILE_EXTENSIONS;

  ngOnInit(): void {
    this.initForm();
    this.loadMyApplications();

    // Watch category changes
    this.uploadForm.get('category')?.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(category => {
      this.filteredDocumentTypes = getDocumentTypesByCategory(category);
      this.uploadForm.patchValue({ documentType: '' });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initForm(): void {
    this.uploadForm = this.fb.group({
      applicationId: ['', Validators.required],
      category: ['', Validators.required],
      documentType: ['', Validators.required],
      description: ['']
    });
  }

  loadMyApplications(): void {
    this.loading = true;
    this.loanService.getMyApplications().subscribe({
      next: (response) => {
        this.myApplications = response.content || [];
        this.loading = false;

        // Pre-select if only one application
        if (this.myApplications.length === 1) {
          this.uploadForm.patchValue({ applicationId: this.myApplications[0].id });
        }

        // Pre-select from route param
        const appId = this.route.snapshot.paramMap.get('appId');
        if (appId) {
          this.uploadForm.patchValue({ applicationId: appId });
        }
      },
      error: () => {
        this.loading = false;
        this.showError('Failed to load applications');
      }
    });
  }

  onApplicationChange(event: any): void {
    // Could load application-specific required documents here
  }

  getLoanTypeLabel(type: string): string {
    const types: Record<string, string> = {
      HOME_LOAN: 'Home Loan',
      PERSONAL_LOAN: 'Personal Loan',
      VEHICLE_LOAN: 'Vehicle Loan',
      BUSINESS_LOAN: 'Business Loan',
      EDUCATION_LOAN: 'Education Loan',
      GOLD_LOAN: 'Gold Loan',
      LAP: 'Loan Against Property'
    };
    return types[type] || type;
  }

  // ============ FILE HANDLING ============

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.validateAndSetFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.validateAndSetFile(event.dataTransfer.files[0]);
    }
  }

  private validateAndSetFile(file: File): void {
    if (!isFileTypeAllowed(file.type)) {
      this.showError(`Invalid file type. Allowed: ${this.allowedExtensions.join(', ')}`);
      return;
    }

    if (!isFileSizeAllowed(file.size)) {
      this.showError(`File too large. Maximum size: ${this.maxFileSizeDisplay}`);
      return;
    }

    this.selectedFile = file;
  }

  removeFile(): void {
    this.selectedFile = null;
    this.uploadProgress = 0;
  }

  getFileIcon(file: File): string {
    if (file.type.includes('pdf')) return 'picture_as_pdf';
    if (file.type.includes('image')) return 'image';
    return 'insert_drive_file';
  }

  formatSize(bytes: number): string {
    return formatFileSize(bytes);
  }

  // ============ UPLOAD ============

  onSubmit(): void {
    if (this.uploadForm.invalid || !this.selectedFile) {
      this.uploadForm.markAllAsTouched();
      if (!this.selectedFile) {
        this.showError('Please select a file to upload');
      }
      return;
    }

    const user = this.authService.getCurrentUserSync();
    const request: DocumentUploadRequest = {
      applicationId: this.uploadForm.value.applicationId,
      documentType: this.uploadForm.value.documentType,
      description: this.uploadForm.value.description || undefined
    };

    // Add customer email for tracking
    if (user?.email) {
      (request as any).customerEmail = user.email;
    }

    this.isUploading = true;
    this.uploadProgress = 0;

    this.documentService.upload(this.selectedFile, request).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const total = event.total || 0;
          this.uploadProgress = total > 0 ? Math.round((100 * event.loaded) / total) : 0;
        } else if (event.type === HttpEventType.Response) {
          this.isUploading = false;
          this.showSuccess('Document uploaded successfully!');
          this.router.navigate(['/my-portal']);
        }
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadProgress = 0;
        this.showError(error.error?.message || 'Upload failed. Please try again.');
      }
    });
  }

  // ============ HELPERS ============

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 3000 });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 5000 });
  }
}
