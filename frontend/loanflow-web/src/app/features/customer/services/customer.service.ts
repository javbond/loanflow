import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer, CustomerRequest, ApiResponse, PageResponse } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private apiUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  // Create a new customer
  create(customer: CustomerRequest): Observable<ApiResponse<Customer>> {
    return this.http.post<ApiResponse<Customer>>(this.apiUrl, customer);
  }

  // Get customer by ID
  getById(id: string): Observable<ApiResponse<Customer>> {
    return this.http.get<ApiResponse<Customer>>(`${this.apiUrl}/${id}`);
  }

  // Get customer by customer number
  getByCustomerNumber(customerNumber: string): Observable<ApiResponse<Customer>> {
    return this.http.get<ApiResponse<Customer>>(`${this.apiUrl}/number/${customerNumber}`);
  }

  // List all customers with pagination
  list(page: number = 0, size: number = 10, sort: string = 'createdAt,desc'): Observable<PageResponse<Customer>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<PageResponse<Customer>>(this.apiUrl, { params });
  }

  // Search customers
  search(query: string, page: number = 0, size: number = 10): Observable<PageResponse<Customer>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Customer>>(`${this.apiUrl}/search`, { params });
  }

  // Update customer
  update(id: string, customer: Partial<CustomerRequest>): Observable<ApiResponse<Customer>> {
    return this.http.put<ApiResponse<Customer>>(`${this.apiUrl}/${id}`, customer);
  }

  // Verify Aadhaar
  verifyAadhaar(id: string): Observable<ApiResponse<Customer>> {
    return this.http.post<ApiResponse<Customer>>(`${this.apiUrl}/${id}/verify-aadhaar`, {});
  }

  // Verify PAN
  verifyPan(id: string): Observable<ApiResponse<Customer>> {
    return this.http.post<ApiResponse<Customer>>(`${this.apiUrl}/${id}/verify-pan`, {});
  }

  // Deactivate customer
  deactivate(id: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  // Activate customer
  activate(id: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/activate`, {});
  }
}
