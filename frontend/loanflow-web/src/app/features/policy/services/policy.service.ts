import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PolicyResponse,
  PolicyRequest,
  ApiResponse,
  PageResponse,
  PolicyStatsResponse
} from '../models/policy.model';

@Injectable({
  providedIn: 'root'
})
export class PolicyService {
  private apiUrl = '/api/v1/policies';

  constructor(private http: HttpClient) {}

  // ==================== CRUD ====================

  create(request: PolicyRequest): Observable<ApiResponse<PolicyResponse>> {
    return this.http.post<ApiResponse<PolicyResponse>>(this.apiUrl, request);
  }

  getById(id: string): Observable<ApiResponse<PolicyResponse>> {
    return this.http.get<ApiResponse<PolicyResponse>>(`${this.apiUrl}/${id}`);
  }

  getByCode(policyCode: string): Observable<ApiResponse<PolicyResponse>> {
    return this.http.get<ApiResponse<PolicyResponse>>(`${this.apiUrl}/code/${policyCode}`);
  }

  update(id: string, request: PolicyRequest): Observable<ApiResponse<PolicyResponse>> {
    return this.http.put<ApiResponse<PolicyResponse>>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  // ==================== Listing & Search ====================

  list(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<ApiResponse<PageResponse<PolicyResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<ApiResponse<PageResponse<PolicyResponse>>>(this.apiUrl, { params });
  }

  listByCategory(category: string, page: number = 0, size: number = 20): Observable<ApiResponse<PageResponse<PolicyResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<PolicyResponse>>>(`${this.apiUrl}/category/${category}`, { params });
  }

  search(query: string, page: number = 0, size: number = 20): Observable<ApiResponse<PageResponse<PolicyResponse>>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<PolicyResponse>>>(`${this.apiUrl}/search`, { params });
  }

  // ==================== Lifecycle ====================

  activate(id: string): Observable<ApiResponse<PolicyResponse>> {
    return this.http.patch<ApiResponse<PolicyResponse>>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<ApiResponse<PolicyResponse>> {
    return this.http.patch<ApiResponse<PolicyResponse>>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  createNewVersion(id: string): Observable<ApiResponse<PolicyResponse>> {
    return this.http.post<ApiResponse<PolicyResponse>>(`${this.apiUrl}/${id}/versions`, {});
  }

  // ==================== Queries ====================

  getVersionHistory(policyCode: string): Observable<ApiResponse<PolicyResponse[]>> {
    return this.http.get<ApiResponse<PolicyResponse[]>>(`${this.apiUrl}/code/${policyCode}/versions`);
  }

  getActivePolicies(loanType: string): Observable<ApiResponse<PolicyResponse[]>> {
    return this.http.get<ApiResponse<PolicyResponse[]>>(`${this.apiUrl}/active/${loanType}`);
  }

  getStats(): Observable<ApiResponse<PolicyStatsResponse>> {
    return this.http.get<ApiResponse<PolicyStatsResponse>>(`${this.apiUrl}/stats`);
  }
}
