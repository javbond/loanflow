import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DocumentService } from '../../services/document.service';
import {
  Document,
  getDocumentStatusInfo,
  getDocumentTypeInfo,
  getDocumentCategoryInfo,
  isDocumentVerifiable
} from '../../models/document.model';

@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatListModule,
    MatTooltipModule
  ],
  templateUrl: './document-viewer.component.html',
  styleUrl: './document-viewer.component.scss'
})
export class DocumentViewerComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private documentService = inject(DocumentService);
  private fb = inject(FormBuilder);
  private sanitizer = inject(DomSanitizer);

  document: Document | null = null;
  loading = true;
  downloadUrl: string | null = null;
  previewUrl: SafeResourceUrl | null = null;

  // Verification
  verificationForm!: FormGroup;
  showVerificationPanel = false;
  isVerifying = false;

  // Mock verifier ID for UAT (will be replaced with auth user ID in Sprint 3)
  currentUserId = '00000000-0000-0000-0000-000000000001';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const action = this.route.snapshot.queryParamMap.get('action');

    if (action === 'verify') {
      this.showVerificationPanel = true;
    }

    this.verificationForm = this.fb.group({
      remarks: ['', Validators.maxLength(500)]
    });

    if (id) {
      this.loadDocument(id);
    }
  }

  loadDocument(id: string): void {
    this.loading = true;

    this.documentService.getById(id).subscribe({
      next: (doc) => {
        this.document = doc;
        this.loading = false;
        this.loadPreview();
      },
      error: (error) => {
        this.loading = false;
        this.showError('Failed to load document');
        console.error('Error loading document:', error);
      }
    });
  }

  loadPreview(): void {
    if (!this.document) return;

    this.documentService.getDownloadUrl(this.document.id).subscribe({
      next: (url) => {
        this.downloadUrl = url;
        // Only set preview for images and PDFs
        if (this.canPreview()) {
          this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        }
      },
      error: () => {
        // Silently fail - preview is optional
      }
    });
  }

  // ============ VERIFICATION ============

  toggleVerificationPanel(): void {
    this.showVerificationPanel = !this.showVerificationPanel;
  }

  approveDocument(): void {
    if (!this.document) return;

    this.isVerifying = true;
    const remarks = this.verificationForm.value.remarks;

    this.documentService.approve(this.document.id, this.currentUserId, remarks).subscribe({
      next: (doc) => {
        this.document = doc;
        this.isVerifying = false;
        this.showVerificationPanel = false;
        this.showSuccess('Document approved successfully');
      },
      error: () => {
        this.isVerifying = false;
        this.showError('Failed to approve document');
      }
    });
  }

  rejectDocument(): void {
    if (!this.document) return;

    const remarks = this.verificationForm.value.remarks;
    if (!remarks) {
      this.showError('Please provide rejection remarks');
      return;
    }

    this.isVerifying = true;

    this.documentService.reject(this.document.id, this.currentUserId, remarks).subscribe({
      next: (doc) => {
        this.document = doc;
        this.isVerifying = false;
        this.showVerificationPanel = false;
        this.showSuccess('Document rejected');
      },
      error: () => {
        this.isVerifying = false;
        this.showError('Failed to reject document');
      }
    });
  }

  // ============ ACTIONS ============

  downloadDocument(): void {
    if (this.downloadUrl) {
      window.open(this.downloadUrl, '_blank');
    } else if (this.document) {
      this.documentService.getDownloadUrl(this.document.id).subscribe({
        next: (url) => {
          window.open(url, '_blank');
        },
        error: () => {
          this.showError('Failed to download document');
        }
      });
    }
  }

  deleteDocument(): void {
    if (!this.document) return;

    if (confirm(`Are you sure you want to delete ${this.document.originalFileName}?`)) {
      this.documentService.delete(this.document.id).subscribe({
        next: () => {
          this.showSuccess('Document deleted');
          this.router.navigate(['/documents/application', this.document?.applicationId]);
        },
        error: () => {
          this.showError('Failed to delete document');
        }
      });
    }
  }

  goBack(): void {
    if (this.document?.applicationId) {
      this.router.navigate(['/documents/application', this.document.applicationId]);
    } else {
      this.router.navigate(['/documents']);
    }
  }

  // ============ HELPERS ============

  canPreview(): boolean {
    if (!this.document) return false;
    const type = this.document.contentType;
    return type === 'application/pdf' ||
           type.startsWith('image/');
  }

  isImage(): boolean {
    return this.document?.contentType.startsWith('image/') || false;
  }

  isPdf(): boolean {
    return this.document?.contentType === 'application/pdf';
  }

  canVerify(): boolean {
    return this.document ? isDocumentVerifiable(this.document.status) : false;
  }

  getStatusInfo(status: string) {
    return getDocumentStatusInfo(status);
  }

  getTypeInfo(type: string) {
    return getDocumentTypeInfo(type);
  }

  getCategoryInfo(category: string) {
    return getDocumentCategoryInfo(category);
  }

  getStatusChipClass(status: string): string {
    const info = getDocumentStatusInfo(status);
    switch (info?.color) {
      case 'accent': return 'status-verified';
      case 'warn': return 'status-warning';
      default: return 'status-default';
    }
  }

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
}
