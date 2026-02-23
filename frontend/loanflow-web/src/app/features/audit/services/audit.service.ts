import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditEvent, AuditEventPage, ApiResponse } from '../models/audit.model';

/**
 * Service for querying audit trail events (US-030).
 * Calls document-service audit REST endpoints.
 */
@Injectable({ providedIn: 'root' })
export class AuditService {

  private readonly apiUrl = '/api/v1/audit';

  constructor(private http: HttpClient) {}

  /**
   * Get audit events for a loan application (paginated).
   */
  getByApplicationId(applicationId: string, page = 0, size = 50): Observable<ApiResponse<AuditEventPage>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<AuditEventPage>>(
      `${this.apiUrl}/application/${applicationId}`,
      { params }
    );
  }

  /**
   * Filter audit events by type and/or date range.
   */
  filterEvents(
    applicationId: string,
    eventType?: string,
    from?: string,
    to?: string
  ): Observable<ApiResponse<AuditEvent[]>> {
    let params = new HttpParams();
    if (eventType) params = params.set('eventType', eventType);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);

    return this.http.get<ApiResponse<AuditEvent[]>>(
      `${this.apiUrl}/application/${applicationId}/filter`,
      { params }
    );
  }

  /**
   * Get audit events by user.
   */
  getByUserId(userId: string, page = 0, size = 50): Observable<ApiResponse<AuditEventPage>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<AuditEventPage>>(
      `${this.apiUrl}/user/${userId}`,
      { params }
    );
  }

  /**
   * Get audit events by customer.
   */
  getByCustomerId(customerId: string, page = 0, size = 50): Observable<ApiResponse<AuditEventPage>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<AuditEventPage>>(
      `${this.apiUrl}/customer/${customerId}`,
      { params }
    );
  }
}
