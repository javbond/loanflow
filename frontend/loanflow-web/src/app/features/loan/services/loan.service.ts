import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LoanApplication,
  LoanApplicationRequest,
  ApiResponse,
  PageResponse,
  LoanStatus
} from '../models/loan.model';

@Injectable({
  providedIn: 'root'
})
export class LoanService {
  private apiUrl = '/api/v1/loans';

  constructor(private http: HttpClient) {}

  // Create a new loan application
  create(loan: LoanApplicationRequest): Observable<ApiResponse<LoanApplication>> {
    return this.http.post<ApiResponse<LoanApplication>>(this.apiUrl, loan);
  }

  // Get loan application by ID
  getById(id: string): Observable<ApiResponse<LoanApplication>> {
    return this.http.get<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}`);
  }

  // Get loan application by application number
  getByApplicationNumber(applicationNumber: string): Observable<ApiResponse<LoanApplication>> {
    return this.http.get<ApiResponse<LoanApplication>>(`${this.apiUrl}/number/${applicationNumber}`);
  }

  // List all loan applications with pagination
  list(page: number = 0, size: number = 10, sort: string = 'createdAt,desc'): Observable<PageResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<PageResponse<LoanApplication>>(this.apiUrl, { params });
  }

  // Search loans by application number (for autocomplete)
  searchByNumber(query: string, page: number = 0, size: number = 10): Observable<PageResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<LoanApplication>>(`${this.apiUrl}/search`, { params });
  }

  // Get loans by customer ID
  getByCustomerId(customerId: string, page: number = 0, size: number = 10): Observable<PageResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<LoanApplication>>(`${this.apiUrl}/customer/${customerId}`, { params });
  }

  // Get loans by status
  getByStatus(status: LoanStatus, page: number = 0, size: number = 10): Observable<PageResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<LoanApplication>>(`${this.apiUrl}/status/${status}`, { params });
  }

  // Update loan application (only DRAFT or RETURNED status)
  update(id: string, loan: Partial<LoanApplicationRequest>): Observable<ApiResponse<LoanApplication>> {
    return this.http.put<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}`, loan);
  }

  // Submit loan application for processing
  submit(id: string): Observable<ApiResponse<LoanApplication>> {
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/submit`, {});
  }

  // Approve loan application
  approve(id: string, approvedAmount: number, interestRate: number): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('approvedAmount', approvedAmount.toString())
      .set('interestRate', interestRate.toString());
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/approve`, {}, { params });
  }

  // Conditionally approve loan application
  conditionallyApprove(
    id: string,
    approvedAmount: number,
    interestRate: number,
    conditions: string
  ): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('approvedAmount', approvedAmount.toString())
      .set('interestRate', interestRate.toString())
      .set('conditions', conditions);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/conditional-approve`, {}, { params });
  }

  // Reject loan application
  reject(id: string, reason: string): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/reject`, {}, { params });
  }

  // Return for correction
  returnForCorrection(id: string, reason: string): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/return`, {}, { params });
  }

  // Cancel loan application
  cancel(id: string, reason: string): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('reason', reason);
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`, { params });
  }

  // Assign loan officer
  assignOfficer(id: string, officerId: string): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams().set('officerId', officerId);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/assign`, {}, { params });
  }

  // Transition status
  transitionStatus(id: string, newStatus: LoanStatus): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams().set('newStatus', newStatus);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/transition`, {}, { params });
  }

  // Update CIBIL score
  updateCibilScore(id: string, cibilScore: number, riskCategory: string): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('cibilScore', cibilScore.toString())
      .set('riskCategory', riskCategory);
    return this.http.patch<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/cibil`, {}, { params });
  }

  // Calculate EMI
  calculateEmi(principal: number, annualRate: number, tenureMonths: number): number {
    if (principal <= 0 || tenureMonths <= 0 || annualRate <= 0) {
      return 0;
    }
    const monthlyRate = annualRate / 1200;
    const onePlusR = 1 + monthlyRate;
    const power = Math.pow(onePlusR, tenureMonths);
    const numerator = principal * monthlyRate * power;
    const denominator = power - 1;
    return Math.round((numerator / denominator) * 100) / 100;
  }

  // ==================== US-023: DOCUMENT GENERATION ====================

  /**
   * Generate sanction letter PDF for an approved/disbursed loan (US-023)
   */
  generateSanctionLetter(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/generate/sanction-letter`, {
      responseType: 'blob'
    });
  }

  // ==================== CUSTOMER PORTAL METHODS ====================
  // Issue: #26 [US-024] Customer Loan Application Form

  // Get my loan applications (Customer Portal)
  getMyApplications(page: number = 0, size: number = 10): Observable<PageResponse<LoanApplication>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<LoanApplication>>(`${this.apiUrl}/my-applications`, { params });
  }

  // Submit loan application (Customer Portal)
  applyForLoan(request: CustomerLoanApplicationRequest): Observable<ApiResponse<LoanApplication>> {
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/apply`, request);
  }

  // Accept loan offer (Customer Portal)
  acceptOffer(id: string): Observable<ApiResponse<LoanApplication>> {
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/accept-offer`, {});
  }

  // Reject loan offer (Customer Portal)
  rejectOffer(id: string, reason: string): Observable<ApiResponse<LoanApplication>> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<ApiResponse<LoanApplication>>(`${this.apiUrl}/${id}/reject-offer`, {}, { params });
  }
}

// Customer loan application request interface
export interface CustomerLoanApplicationRequest {
  loanType: string;
  requestedAmount: number;
  tenureMonths: number;
  purpose?: string;
  fullName: string;
  pan: string;
  aadhaar?: string;
  phone: string;
  email: string;
  address?: string;
  employmentType: string;
  employerName?: string;
  monthlyIncome: number;
}
