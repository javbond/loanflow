import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IncomeVerificationResponse, IncomeVerificationRequest } from '../models/income-verification.model';

/**
 * HTTP client for the Income Verification REST API.
 * Calls loan-service /api/v1/income-verification endpoints.
 */
@Injectable({
  providedIn: 'root'
})
export class IncomeVerificationService {
  private apiUrl = '/api/v1/income-verification';

  constructor(private http: HttpClient) {}

  /**
   * Trigger income verification (or retrieve from cache).
   */
  verify(request: IncomeVerificationRequest): Observable<IncomeVerificationResponse> {
    return this.http.post<IncomeVerificationResponse>(`${this.apiUrl}/verify`, request);
  }

  /**
   * Get cached income verification by PAN.
   */
  getByPan(pan: string): Observable<IncomeVerificationResponse> {
    return this.http.get<IncomeVerificationResponse>(`${this.apiUrl}/${pan}`);
  }
}
