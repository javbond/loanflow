import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { DocumentService } from '../../services/document.service';
import {
  Document,
  DocumentCompletenessResponse,
  VerificationChecklistItem,
  getDocumentTypeInfo,
  getDocumentStatusInfo,
  isDocumentVerifiable
} from '../../models/document.model';

@Component({
  selector: 'app-document-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatBadgeModule,
    MatDividerModule,
    MatExpansionModule
  ],
  templateUrl: './document-panel.component.html',
  styleUrls: ['./document-panel.component.scss']
})
export class DocumentPanelComponent implements OnChanges {
  @Input() applicationId?: string;
  @Input() loanType?: string;

  documents: Document[] = [];
  completeness: DocumentCompletenessResponse | null = null;
  loading = false;
  verifyLoading = false;

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['applicationId'] || changes['loanType']) && this.applicationId) {
      this.loadDocuments();
      if (this.loanType) {
        this.loadCompleteness();
      }
    }
  }

  loadDocuments(): void {
    if (!this.applicationId) return;
    this.loading = true;
    this.documentService.getByApplicationId(this.applicationId, 0, 100).subscribe({
      next: (response) => {
        this.documents = response.content;
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Failed to load documents', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadCompleteness(): void {
    if (!this.applicationId || !this.loanType) return;
    this.documentService.getCompleteness(this.applicationId, this.loanType).subscribe({
      next: (response) => {
        this.completeness = response;
      },
      error: () => {
        // Non-critical — completeness is supplementary
      }
    });
  }

  verifyDocument(doc: Document, approved: boolean): void {
    this.verifyLoading = true;
    const request = {
      verifierId: 'current-user', // Will be resolved from JWT on backend
      approved,
      remarks: approved ? 'Approved via document panel' : 'Rejected via document panel'
    };

    this.documentService.verify(doc.id, request).subscribe({
      next: () => {
        this.snackBar.open(
          `Document ${approved ? 'verified' : 'rejected'} successfully`,
          'Close',
          { duration: 3000 }
        );
        this.loadDocuments();
        if (this.loanType) this.loadCompleteness();
        this.verifyLoading = false;
      },
      error: () => {
        this.snackBar.open('Verification failed', 'Close', { duration: 3000 });
        this.verifyLoading = false;
      }
    });
  }

  batchVerifyAll(): void {
    const uploadedDocs = this.documents.filter(d => d.status === 'UPLOADED');
    if (uploadedDocs.length === 0) return;

    this.verifyLoading = true;
    this.documentService.batchVerify({
      documentIds: uploadedDocs.map(d => d.id),
      verifierId: 'current-user',
      approved: true,
      remarks: 'Batch verified via document panel'
    }).subscribe({
      next: () => {
        this.snackBar.open(`${uploadedDocs.length} documents verified`, 'Close', { duration: 3000 });
        this.loadDocuments();
        if (this.loanType) this.loadCompleteness();
        this.verifyLoading = false;
      },
      error: () => {
        this.snackBar.open('Batch verification failed', 'Close', { duration: 3000 });
        this.verifyLoading = false;
      }
    });
  }

  downloadDocument(doc: Document): void {
    this.documentService.getDownloadUrl(doc.id).subscribe({
      next: (url) => {
        window.open(url, '_blank');
      },
      error: () => {
        this.snackBar.open('Failed to generate download URL', 'Close', { duration: 3000 });
      }
    });
  }

  getStatusIcon(status: string): string {
    return getDocumentStatusInfo(status)?.icon || 'description';
  }

  getStatusColor(status: string): string {
    const info = getDocumentStatusInfo(status);
    switch (status) {
      case 'VERIFIED': return '#4caf50';
      case 'REJECTED': return '#f44336';
      case 'UPLOADED': return '#2196f3';
      default: return '#9e9e9e';
    }
  }

  getTypeLabel(type: string): string {
    return getDocumentTypeInfo(type)?.label || type;
  }

  isVerifiable(status: string): boolean {
    return isDocumentVerifiable(status);
  }

  get uploadedCount(): number {
    return this.documents.filter(d => d.status === 'UPLOADED').length;
  }

  getChecklistIcon(item: VerificationChecklistItem): string {
    if (item.verified) return 'check_circle';
    if (item.rejected) return 'cancel';
    if (item.uploaded) return 'cloud_done';
    return 'radio_button_unchecked';
  }

  getChecklistColor(item: VerificationChecklistItem): string {
    if (item.verified) return '#4caf50';
    if (item.rejected) return '#f44336';
    if (item.uploaded) return '#2196f3';
    return '#9e9e9e';
  }
}
