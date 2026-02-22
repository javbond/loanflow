import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreditBureauResponse, CreditPullRequest } from '../models/credit-bureau.model';

/**
 * HTTP client for the Credit Bureau REST API.
 * Calls loan-service /api/v1/credit-bureau endpoints.
 */
@Injectable({
  providedIn: 'root'
})
export class CreditBureauService {
  private apiUrl = '/api/v1/credit-bureau';

  constructor(private http: HttpClient) {}

  /**
   * Trigger a credit bureau pull (or retrieve from cache).
   */
  pullReport(request: CreditPullRequest): Observable<CreditBureauResponse> {
    return this.http.post<CreditBureauResponse>(`${this.apiUrl}/pull`, request);
  }

  /**
   * Get cached credit bureau report by PAN.
   */
  getByPan(pan: string): Observable<CreditBureauResponse> {
    return this.http.get<CreditBureauResponse>(`${this.apiUrl}/${pan}`);
  }
}
