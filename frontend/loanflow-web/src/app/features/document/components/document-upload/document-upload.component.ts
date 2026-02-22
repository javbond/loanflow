import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Subject, debounceTime, distinctUntilChanged, filter, switchMap, takeUntil } from 'rxjs';

import { DocumentService } from '../../services/document.service';
import { LoanService } from '../../../loan/services/loan.service';
import { LoanApplication } from '../../../loan/models/loan.model';
import {
  DocumentUploadRequest,
  DOCUMENT_TYPES,
  DOCUMENT_CATEGORIES,
  DocumentTypeInfo,
  DocumentCategoryInfo,
  MAX_FILE_SIZE,
  MAX_FILE_SIZE_DISPLAY,
  ALLOWED_FILE_EXTENSIONS,
  isFileTypeAllowed,
  isFileSizeAllowed,
  formatFileSize,
  getDocumentTypesByCategory
} from '../../models/document.model';

@Component({
  selector: 'app-document-upload',
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
    MatDividerModule,
    MatAutocompleteModule
  ],
  templateUrl: './document-upload.component.html',
  styleUrl: './document-upload.component.scss'
})
export class DocumentUploadComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private documentService = inject(DocumentService);
  private loanService = inject(LoanService);
  private destroy$ = new Subject<void>();

  uploadForm!: FormGroup;
  loanSearchControl = new FormControl('');
  categories = DOCUMENT_CATEGORIES;
  documentTypes = DOCUMENT_TYPES;
  filteredDocumentTypes: DocumentTypeInfo[] = [];

  // Loan search
  loanSearchResults: LoanApplication[] = [];
  selectedLoan: LoanApplication | null = null;
  isSearchingLoans = false;

  // File handling
  selectedFile: File | null = null;
  isDragOver = false;
  uploadProgress = 0;
  isUploading = false;
  uploadStatus: 'idle' | 'scanning' | 'uploading' | 'complete' = 'idle';

  // Validation
  maxFileSize = MAX_FILE_SIZE;
  maxFileSizeDisplay = MAX_FILE_SIZE_DISPLAY;
  allowedExtensions = ALLOWED_FILE_EXTENSIONS;

  // Pre-filled from route
  applicationId: string | null = null;

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('appId');

    this.uploadForm = this.fb.group({
      applicationId: [this.applicationId || '', Validators.required],
      customerId: [''],
      category: ['', Validators.required],
      documentType: ['', Validators.required],
      description: ['']
    });

    // Filter document types when category changes
    this.uploadForm.get('category')?.valueChanges.subscribe(category => {
      this.filteredDocumentTypes = getDocumentTypesByCategory(category);
      this.uploadForm.patchValue({ documentType: '' });
    });

    // Setup loan search autocomplete
    this.setupLoanSearch();

    // If applicationId is provided in route, load that loan
    if (this.applicationId) {
      this.loadLoanById(this.applicationId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupLoanSearch(): void {
    this.loanSearchControl.valueChanges.pipe(
      takeUntil(this.destroy$),
      debounceTime(300),
      distinctUntilChanged(),
      filter(value => typeof value === 'string' && value.length >= 2),
      switchMap(query => {
        this.isSearchingLoans = true;
        return this.loanService.searchByNumber(query as string);
      })
    ).subscribe({
      next: (response) => {
        this.loanSearchResults = response.content;
        this.isSearchingLoans = false;
      },
      error: () => {
        this.loanSearchResults = [];
        this.isSearchingLoans = false;
      }
    });
  }

  private loadLoanById(id: string): void {
    this.loanService.getById(id).subscribe({
      next: (response) => {
        if (response.data) {
          this.selectedLoan = response.data;
          this.loanSearchControl.setValue(response.data.applicationNumber || '');
          this.uploadForm.patchValue({ applicationId: response.data.id });
        }
      },
      error: () => {
        // If not a UUID, try by application number
        this.loanService.getByApplicationNumber(id).subscribe({
          next: (response) => {
            if (response.data) {
              this.selectedLoan = response.data;
              this.loanSearchControl.setValue(response.data.applicationNumber || '');
              this.uploadForm.patchValue({ applicationId: response.data.id });
            }
          },
          error: () => this.showError('Could not find loan application')
        });
      }
    });
  }

  // Loan autocomplete methods
  displayLoanFn(loan: LoanApplication): string {
    return loan ? `${loan.applicationNumber} - ${loan.customerName || 'N/A'}` : '';
  }

  onLoanSelected(loan: LoanApplication): void {
    this.selectedLoan = loan;
    this.uploadForm.patchValue({
      applicationId: loan.id,
      customerId: loan.customerId || ''
    });
  }

  clearLoanSelection(): void {
    this.selectedLoan = null;
    this.loanSearchControl.setValue('');
    this.uploadForm.patchValue({ applicationId: '', customerId: '' });
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
    // Validate file type
    if (!isFileTypeAllowed(file.type)) {
      this.showError(`Invalid file type. Allowed: ${this.allowedExtensions.join(', ')}`);
      return;
    }

    // Validate file size
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
    if (file.type.includes('word') || file.type.includes('document')) return 'description';
    return 'insert_drive_file';
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

    const request: DocumentUploadRequest = {
      applicationId: this.uploadForm.value.applicationId,
      customerId: this.uploadForm.value.customerId || undefined,
      documentType: this.uploadForm.value.documentType,
      description: this.uploadForm.value.description || undefined
    };

    this.isUploading = true;
    this.uploadProgress = 0;
    this.uploadStatus = 'scanning';

    this.documentService.upload(this.selectedFile, request).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.uploadStatus = 'uploading';
          const total = event.total || 0;
          this.uploadProgress = total > 0 ? Math.round((100 * event.loaded) / total) : 0;
        } else if (event.type === HttpEventType.Response) {
          this.uploadStatus = 'complete';
          this.isUploading = false;
          this.showSuccess('Document uploaded successfully!');

          // Navigate to document list for this application
          const appId = this.uploadForm.value.applicationId;
          this.router.navigate(['/documents/application', appId]);
        }
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadStatus = 'idle';
        this.uploadProgress = 0;

        // Check for virus scan specific errors
        const errorCode = error.error?.error?.code;
        if (errorCode === 'VIRUS_DETECTED') {
          this.showError('Security Warning: A virus was detected in this file. Please scan your device with antivirus software and upload a clean file.');
        } else if (errorCode === 'VIRUS_SCAN_FAILED') {
          this.showError('Unable to verify file safety. Please try uploading again.');
        } else {
          this.showError(error.error?.message || 'Upload failed. Please try again.');
        }
      }
    });
  }

  // ============ HELPERS ============

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  formatSize(bytes: number): string {
    return formatFileSize(bytes);
  }
}
