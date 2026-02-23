import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../models/customer.model';
import {
  EkycInitiateResponse,
  EkycVerifyRequest,
  EkycVerifyResponse,
  KycStatusResponse
} from '../models/ekyc.model';

/**
 * Angular service for e-KYC verification API (US-029).
 */
@Injectable({ providedIn: 'root' })
export class EkycService {

  private apiUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  /**
   * Initiate e-KYC OTP verification for a customer.
   */
  initiateEkyc(customerId: string, aadhaarNumber: string): Observable<ApiResponse<EkycInitiateResponse>> {
    return this.http.post<ApiResponse<EkycInitiateResponse>>(
      `${this.apiUrl}/${customerId}/ekyc/initiate`,
      { aadhaarNumber }
    );
  }

  /**
   * Verify OTP and retrieve e-KYC demographic data.
   */
  verifyOtp(customerId: string, request: EkycVerifyRequest): Observable<ApiResponse<EkycVerifyResponse>> {
    return this.http.post<ApiResponse<EkycVerifyResponse>>(
      `${this.apiUrl}/${customerId}/ekyc/verify`,
      request
    );
  }

  /**
   * Get current KYC verification status for a customer.
   */
  getKycStatus(customerId: string): Observable<ApiResponse<KycStatusResponse>> {
    return this.http.get<ApiResponse<KycStatusResponse>>(
      `${this.apiUrl}/${customerId}/ekyc/status`
    );
  }
}
