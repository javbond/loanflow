import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { DocumentService } from '../../../document/services/document.service';
import { Document, DOCUMENT_TYPES, DOCUMENT_STATUSES } from '../../../document/models/document.model';

/**
 * My Documents Component
 * Customer view for managing their uploaded documents
 * Issue: #29 [US-027] Document Download
 */
@Component({
  selector: 'app-my-documents',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatPaginatorModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="documents-container">
      <div class="header-section">
        <div class="title-section">
          <h1><mat-icon>folder_open</mat-icon> My Documents</h1>
          <p class="subtitle">View and download your uploaded documents</p>
        </div>
        <button mat-raised-button color="primary" routerLink="/my-portal/documents/upload">
          <mat-icon>cloud_upload</mat-icon> Upload New Document
        </button>
      </div>

      @if (loading) {
        <div class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Loading your documents...</p>
        </div>
      } @else if (documents.length === 0) {
        <mat-card class="empty-state-card">
          <mat-card-content>
            <div class="empty-state">
              <mat-icon class="empty-icon">folder_off</mat-icon>
              <h2>No Documents Yet</h2>
              <p>You haven't uploaded any documents. Upload documents to support your loan applications.</p>
              <button mat-raised-button color="primary" routerLink="/my-portal/documents/upload">
                <mat-icon>cloud_upload</mat-icon> Upload Your First Document
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      } @else {
        <mat-card class="documents-card">
          <mat-card-content>
            <div class="documents-grid">
              @for (doc of documents; track doc.id) {
                <div class="document-card">
                  <div class="doc-icon-section">
                    <mat-icon class="doc-icon" [class]="getIconClass(doc.contentType)">
                      {{ getFileIcon(doc.contentType) }}
                    </mat-icon>
                  </div>
                  <div class="doc-info">
                    <h3 class="doc-name" [matTooltip]="doc.originalFileName || 'Unknown'">
                      {{ truncateFilename(doc.originalFileName) }}
                    </h3>
                    <p class="doc-type">{{ getDocumentTypeLabel(doc.documentType) }}</p>
                    <div class="doc-meta">
                      <span class="meta-item">
                        <mat-icon>straighten</mat-icon>
                        {{ formatFileSize(doc.fileSize) }}
                      </span>
                      <span class="meta-item">
                        <mat-icon>schedule</mat-icon>
                        {{ formatDate(doc.createdAt) }}
                      </span>
                    </div>
                    <mat-chip [class]="'status-chip ' + doc.status?.toLowerCase()" size="small">
                      {{ getStatusLabel(doc.status) }}
                    </mat-chip>
                  </div>
                  <div class="doc-actions">
                    <button mat-icon-button color="primary"
                            (click)="downloadDocument(doc)"
                            [disabled]="downloadingId === doc.id"
                            matTooltip="Download">
                      @if (downloadingId === doc.id) {
                        <mat-spinner diameter="24"></mat-spinner>
                      } @else {
                        <mat-icon>download</mat-icon>
                      }
                    </button>
                  </div>
                </div>
              }
            </div>

            <mat-paginator
              [length]="totalElements"
              [pageSize]="pageSize"
              [pageIndex]="pageIndex"
              [pageSizeOptions]="[10, 20, 50]"
              (page)="onPageChange($event)"
              showFirstLastButtons>
            </mat-paginator>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .documents-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .header-section { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
    .title-section h1 { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.75rem; color: #1976d2; }
    .subtitle { color: #666; margin: 8px 0 0; }
    .loading-container { display: flex; flex-direction: column; align-items: center; padding: 48px; color: #666; }
    .loading-container p { margin-top: 16px; }
    .empty-state-card { max-width: 500px; margin: 48px auto; }
    .empty-state { text-align: center; padding: 32px; }
    .empty-icon { font-size: 72px; width: 72px; height: 72px; color: #ccc; }
    .empty-state h2 { margin: 16px 0 8px; color: #333; }
    .empty-state p { color: #666; margin-bottom: 24px; }
    .documents-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .document-card { display: flex; align-items: center; gap: 16px; padding: 16px; border: 1px solid #e0e0e0; border-radius: 8px; background: #fafafa; transition: box-shadow 0.2s; }
    .document-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .doc-icon-section { flex-shrink: 0; }
    .doc-icon { font-size: 40px; width: 40px; height: 40px; color: #1976d2; }
    .doc-icon.pdf { color: #f44336; }
    .doc-icon.image { color: #4caf50; }
    .doc-icon.word { color: #2196f3; }
    .doc-info { flex: 1; min-width: 0; }
    .doc-name { margin: 0 0 4px; font-size: 1rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .doc-type { margin: 0 0 8px; font-size: 0.875rem; color: #666; }
    .doc-meta { display: flex; gap: 16px; margin-bottom: 8px; }
    .meta-item { display: flex; align-items: center; gap: 4px; font-size: 0.75rem; color: #888; }
    .meta-item mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .doc-actions { flex-shrink: 0; }
    .status-chip { font-size: 0.7rem; }
    .status-chip.uploaded { background: #fff3e0; color: #e65100; }
    .status-chip.verified { background: #e8f5e9; color: #2e7d32; }
    .status-chip.rejected { background: #ffebee; color: #c62828; }
    mat-paginator { margin-top: 16px; }
    @media (max-width: 600px) { .documents-grid { grid-template-columns: 1fr; } .header-section { flex-direction: column; } }
  `]
})
export class MyDocumentsComponent implements OnInit {
  private documentService = inject(DocumentService);
  private snackBar = inject(MatSnackBar);

  documents: Document[] = [];
  loading = true;
  downloadingId: string | null = null;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.loading = true;
    this.documentService.getMyDocuments(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.documents = response.content || [];
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.documents = [];
        this.snackBar.open('Failed to load documents', 'Close', { duration: 3000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadDocuments();
  }

  downloadDocument(doc: Document): void {
    if (!doc.id) return;

    this.downloadingId = doc.id;
    this.documentService.getMyDocumentDownloadUrl(doc.id).subscribe({
      next: (url) => {
        // Open download URL in new tab
        window.open(url, '_blank');
        this.downloadingId = null;
        this.snackBar.open('Download started', 'Close', { duration: 2000 });
      },
      error: () => {
        this.downloadingId = null;
        this.snackBar.open('Failed to download document', 'Close', { duration: 3000 });
      }
    });
  }

  getFileIcon(contentType?: string): string {
    if (!contentType) return 'insert_drive_file';
    if (contentType.includes('pdf')) return 'picture_as_pdf';
    if (contentType.includes('image')) return 'image';
    if (contentType.includes('word') || contentType.includes('document')) return 'description';
    return 'insert_drive_file';
  }

  getIconClass(contentType?: string): string {
    if (!contentType) return '';
    if (contentType.includes('pdf')) return 'pdf';
    if (contentType.includes('image')) return 'image';
    if (contentType.includes('word')) return 'word';
    return '';
  }

  getDocumentTypeLabel(type?: string): string {
    if (!type) return 'Unknown';
    return DOCUMENT_TYPES.find(t => t.value === type)?.label || type.replace(/_/g, ' ');
  }

  getStatusLabel(status?: string): string {
    if (!status) return 'Unknown';
    return DOCUMENT_STATUSES.find(s => s.value === status)?.label || status;
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }

  truncateFilename(name?: string): string {
    if (!name) return 'Unknown';
    if (name.length <= 30) return name;
    const ext = name.split('.').pop();
    const base = name.substring(0, name.length - (ext?.length || 0) - 1);
    return base.substring(0, 25) + '...' + (ext ? '.' + ext : '');
  }
}
