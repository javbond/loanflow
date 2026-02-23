import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { AuditService } from '../../services/audit.service';
import { AuditEvent, AUDIT_EVENT_CONFIG, AUDIT_EVENT_TYPES } from '../../models/audit.model';

/**
 * Reusable audit timeline component (US-030).
 * Displays a chronological list of audit events for a loan application.
 */
@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatChipsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  templateUrl: './audit-timeline.component.html',
  styleUrl: './audit-timeline.component.scss',
})
export class AuditTimelineComponent implements OnInit, OnChanges {
  @Input() applicationId!: string;

  events: AuditEvent[] = [];
  filteredEvents: AuditEvent[] = [];
  loading = false;
  error: string | null = null;

  // Filters
  selectedEventType = '';
  eventTypes = AUDIT_EVENT_TYPES;
  eventConfig = AUDIT_EVENT_CONFIG;

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    if (this.applicationId) {
      this.loadEvents();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['applicationId'] && !changes['applicationId'].firstChange) {
      this.loadEvents();
    }
  }

  loadEvents(): void {
    if (!this.applicationId) return;

    this.loading = true;
    this.error = null;

    this.auditService.getByApplicationId(this.applicationId).subscribe({
      next: (response) => {
        this.events = response.data?.content || [];
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load audit trail';
        this.loading = false;
        console.error('Audit trail error:', err);
      },
    });
  }

  applyFilter(): void {
    if (this.selectedEventType) {
      this.filteredEvents = this.events.filter(
        (e) => e.eventType === this.selectedEventType
      );
    } else {
      this.filteredEvents = [...this.events];
    }
  }

  onFilterChange(): void {
    this.applyFilter();
  }

  clearFilter(): void {
    this.selectedEventType = '';
    this.applyFilter();
  }

  getEventConfig(eventType: string): { label: string; icon: string; color: string } {
    return (
      this.eventConfig[eventType] || {
        label: eventType.replace(/_/g, ' '),
        icon: 'info',
        color: '#757575',
      }
    );
  }

  formatTimestamp(timestamp: string): string {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  formatRelativeTime(timestamp: string): string {
    if (!timestamp) return '';
    const now = new Date();
    const date = new Date(timestamp);
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return this.formatTimestamp(timestamp);
  }

  getStateChanges(event: AuditEvent): { key: string; before: string; after: string }[] {
    const changes: { key: string; before: string; after: string }[] = [];
    if (!event.beforeState || !event.afterState) return changes;

    for (const key of Object.keys(event.afterState)) {
      const before = event.beforeState[key];
      const after = event.afterState[key];
      if (before !== after && after !== undefined) {
        changes.push({
          key: this.formatKey(key),
          before: before !== undefined ? String(before) : '-',
          after: String(after),
        });
      }
    }
    return changes;
  }

  private formatKey(key: string): string {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (s) => s.toUpperCase())
      .trim();
  }
}
