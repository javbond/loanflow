import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentService } from '../../services/document.service';
import { Document } from '../../models/document.model';

@Component({
  selector: 'app-extraction-review',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatSnackBarModule
  ],
  templateUrl: './extraction-review.component.html',
  styleUrls: ['./extraction-review.component.scss']
})
export class ExtractionReviewComponent implements OnChanges {
  @Input() document?: Document;

  fields: { key: string; value: string }[] = [];
  saving = false;

  constructor(
    private documentService: DocumentService,
    private snackBar: MatSnackBar
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['document'] && this.document?.extractedData) {
      this.fields = Object.entries(this.document.extractedData)
        .map(([key, value]) => ({ key, value }));
    }
  }

  get statusColor(): string {
    switch (this.document?.extractionStatus) {
      case 'SUCCESS': return '#4caf50';
      case 'PARTIAL': return '#ff9800';
      case 'FAILED': return '#f44336';
      case 'REVIEWED': return '#2196f3';
      default: return '#9e9e9e';
    }
  }

  get statusIcon(): string {
    switch (this.document?.extractionStatus) {
      case 'SUCCESS': return 'check_circle';
      case 'PARTIAL': return 'warning';
      case 'FAILED': return 'error';
      case 'REVIEWED': return 'rate_review';
      default: return 'pending';
    }
  }

  saveCorrections(): void {
    if (!this.document?.id) return;

    this.saving = true;
    const data: { [key: string]: string } = {};
    this.fields.forEach(f => data[f.key] = f.value);

    this.documentService.updateExtractedData(this.document.id, data).subscribe({
      next: () => {
        this.snackBar.open('Extracted data updated successfully', 'Close', { duration: 3000 });
        this.saving = false;
      },
      error: () => {
        this.snackBar.open('Failed to update extracted data', 'Close', { duration: 3000 });
        this.saving = false;
      }
    });
  }

  formatFieldKey(key: string): string {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }
}
