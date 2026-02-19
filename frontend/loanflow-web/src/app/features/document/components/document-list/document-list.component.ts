import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { DocumentService, PageResponse } from '../../services/document.service';
import {
  Document,
  DOCUMENT_STATUSES,
  DOCUMENT_CATEGORIES,
  DocumentStatusInfo,
  DocumentCategoryInfo,
  getDocumentStatusInfo,
  getDocumentTypeInfo,
  isDocumentVerifiable
} from '../../models/document.model';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatMenuModule,
    MatDividerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss'
})
export class DocumentListComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private documentService = inject(DocumentService);

  // Table data
  documents: Document[] = [];
  displayedColumns = ['documentNumber', 'documentType', 'fileName', 'fileSize', 'status', 'uploadedAt', 'actions'];

  // Pagination
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  pageSizeOptions = [5, 10, 25, 50];

  // Filters
  applicationId: string | null = null;
  selectedStatus = '';
  selectedCategory = '';
  statuses = DOCUMENT_STATUSES;
  categories = DOCUMENT_CATEGORIES;

  // State
  loading = false;

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('appId');
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;

    let observable;

    if (this.applicationId) {
      if (this.selectedStatus) {
        observable = this.documentService.getByApplicationAndStatus(
          this.applicationId,
          this.selectedStatus,
          this.pageIndex,
          this.pageSize
        );
      } else if (this.selectedCategory) {
        observable = this.documentService.getByApplicationAndCategory(
          this.applicationId,
          this.selectedCategory,
          this.pageIndex,
          this.pageSize
        );
      } else {
        observable = this.documentService.getByApplicationId(
          this.applicationId,
          this.pageIndex,
          this.pageSize
        );
      }
    } else {
      // No application ID - load all documents
      observable = this.documentService.getAll(this.pageIndex, this.pageSize);
    }

    observable.subscribe({
      next: (response: PageResponse<Document>) => {
        this.documents = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        this.showError('Failed to load documents');
        console.error('Error loading documents:', error);
      }
    });
  }

  // ============ PAGINATION ============

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocuments();
  }

  // ============ FILTERS ============

  onStatusFilterChange(): void {
    this.selectedCategory = ''; // Clear category when status is selected
    this.pageIndex = 0;
    this.loadDocuments();
  }

  onCategoryFilterChange(): void {
    this.selectedStatus = ''; // Clear status when category is selected
    this.pageIndex = 0;
    this.loadDocuments();
  }

  clearFilters(): void {
    this.selectedStatus = '';
    this.selectedCategory = '';
    this.pageIndex = 0;
    this.loadDocuments();
  }

  // ============ ACTIONS ============

  viewDocument(doc: Document): void {
    this.router.navigate(['/documents', doc.id]);
  }

  downloadDocument(doc: Document): void {
    this.documentService.getDownloadUrl(doc.id).subscribe({
      next: (url) => {
        window.open(url, '_blank');
      },
      error: () => {
        this.showError('Failed to get download URL');
      }
    });
  }

  uploadDocument(): void {
    if (this.applicationId) {
      this.router.navigate(['/documents/upload', this.applicationId]);
    } else {
      this.router.navigate(['/documents/upload']);
    }
  }

  deleteDocument(doc: Document): void {
    if (confirm(`Are you sure you want to delete ${doc.originalFileName}?`)) {
      this.documentService.delete(doc.id).subscribe({
        next: () => {
          this.showSuccess('Document deleted successfully');
          this.loadDocuments();
        },
        error: () => {
          this.showError('Failed to delete document');
        }
      });
    }
  }

  // ============ HELPERS ============

  getStatusInfo(status: string): DocumentStatusInfo | undefined {
    return getDocumentStatusInfo(status);
  }

  getDocumentTypeLabel(type: string): string {
    return getDocumentTypeInfo(type)?.label || type;
  }

  getStatusChipClass(status: string): string {
    const info = getDocumentStatusInfo(status);
    switch (info?.color) {
      case 'accent': return 'status-verified';
      case 'warn': return 'status-warning';
      default: return 'status-default';
    }
  }

  canVerify(doc: Document): boolean {
    return isDocumentVerifiable(doc.status);
  }

  isExpiringSoon(doc: Document): boolean {
    return doc.expiringSoon || false;
  }

  isExpired(doc: Document): boolean {
    return doc.expired || false;
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
