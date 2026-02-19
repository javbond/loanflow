import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Document, DocumentUploadRequest, DocumentVerificationRequest } from '../models/document.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface UploadProgress {
  progress: number;
  loaded: number;
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly apiUrl = '/api/v1/documents';

  constructor(private http: HttpClient) {}

  // ============ UPLOAD ============

  /**
   * Upload a document with progress tracking
   */
  upload(file: File, request: DocumentUploadRequest): Observable<HttpEvent<ApiResponse<Document>>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    return this.http.post<ApiResponse<Document>>(this.apiUrl, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  /**
   * Simple upload without progress tracking
   */
  uploadSimple(file: File, request: DocumentUploadRequest): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    return this.http.post<ApiResponse<Document>>(this.apiUrl, formData).pipe(
      map(response => response.data)
    );
  }

  // ============ RETRIEVE ============

  /**
   * Get document by ID
   */
  getById(id: string): Observable<Document> {
    return this.http.get<ApiResponse<Document>>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data)
    );
  }

  /**
   * Get document by document number
   */
  getByDocumentNumber(documentNumber: string): Observable<Document> {
    return this.http.get<ApiResponse<Document>>(`${this.apiUrl}/number/${documentNumber}`).pipe(
      map(response => response.data)
    );
  }

  /**
   * Get presigned download URL
   */
  getDownloadUrl(id: string): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.apiUrl}/${id}/download-url`).pipe(
      map(response => response.data)
    );
  }

  // ============ LIST ============

  /**
   * Get all documents with pagination
   */
  getAll(page = 0, size = 20): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Document>>(this.apiUrl, { params });
  }

  /**
   * Get documents by loan application ID
   */
  getByApplicationId(applicationId: string, page = 0, size = 20): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Document>>(
      `${this.apiUrl}/application/${applicationId}`,
      { params }
    );
  }

  /**
   * Get documents by customer ID
   */
  getByCustomerId(customerId: string, page = 0, size = 20): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Document>>(
      `${this.apiUrl}/customer/${customerId}`,
      { params }
    );
  }

  /**
   * Get documents by application and status
   */
  getByApplicationAndStatus(
    applicationId: string,
    status: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Document>>(
      `${this.apiUrl}/application/${applicationId}/status/${status}`,
      { params }
    );
  }

  /**
   * Get documents by application and category
   */
  getByApplicationAndCategory(
    applicationId: string,
    category: string,
    page = 0,
    size = 20
  ): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Document>>(
      `${this.apiUrl}/application/${applicationId}/category/${category}`,
      { params }
    );
  }

  // ============ VERIFICATION ============

  /**
   * Verify or reject a document
   */
  verify(id: string, request: DocumentVerificationRequest): Observable<Document> {
    return this.http.post<ApiResponse<Document>>(`${this.apiUrl}/${id}/verify`, request).pipe(
      map(response => response.data)
    );
  }

  /**
   * Approve a document
   */
  approve(id: string, verifierId: string, remarks?: string): Observable<Document> {
    return this.verify(id, { verifierId, approved: true, remarks });
  }

  /**
   * Reject a document
   */
  reject(id: string, verifierId: string, remarks: string): Observable<Document> {
    return this.verify(id, { verifierId, approved: false, remarks });
  }

  // ============ DELETE ============

  /**
   * Soft delete document
   */
  delete(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).pipe(
      map(() => void 0)
    );
  }

  /**
   * Hard delete document (Admin only)
   */
  hardDelete(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}/hard`).pipe(
      map(() => void 0)
    );
  }

  // ============ STATISTICS ============

  /**
   * Get document count for application
   */
  getCountByApplication(applicationId: string): Observable<number> {
    return this.http.get<ApiResponse<number>>(
      `${this.apiUrl}/application/${applicationId}/count`
    ).pipe(
      map(response => response.data)
    );
  }

  /**
   * Get pending verification count for application
   */
  getPendingCount(applicationId: string): Observable<number> {
    return this.http.get<ApiResponse<number>>(
      `${this.apiUrl}/application/${applicationId}/pending-count`
    ).pipe(
      map(response => response.data)
    );
  }

  // ============ HELPERS ============

  /**
   * Extract upload progress from HttpEvent
   */
  getUploadProgress(event: HttpEvent<any>): UploadProgress | null {
    if (event.type === HttpEventType.UploadProgress) {
      const total = event.total || 0;
      const loaded = event.loaded;
      const progress = total > 0 ? Math.round((100 * loaded) / total) : 0;
      return { progress, loaded, total };
    }
    return null;
  }

  /**
   * Check if upload is complete
   */
  isUploadComplete(event: HttpEvent<any>): boolean {
    return event.type === HttpEventType.Response;
  }

  /**
   * Get response from completed upload
   */
  getUploadResponse(event: HttpEvent<ApiResponse<Document>>): Document | null {
    if (event.type === HttpEventType.Response) {
      return event.body?.data || null;
    }
    return null;
  }

  // ==================== CUSTOMER PORTAL METHODS ====================
  // Issue: #27 [US-025] Customer Document Upload

  /**
   * Get my documents (Customer Portal)
   */
  getMyDocuments(page = 0, size = 20): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Document>>(`${this.apiUrl}/my-documents`, { params });
  }

  /**
   * Get my documents for specific application (Customer Portal)
   */
  getMyDocumentsByApplication(applicationId: string, page = 0, size = 20): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Document>>(
      `${this.apiUrl}/my-documents/application/${applicationId}`,
      { params }
    );
  }

  /**
   * Get download URL for my document (Customer Portal)
   */
  getMyDocumentDownloadUrl(id: string): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.apiUrl}/my-documents/${id}/download-url`).pipe(
      map(response => response.data)
    );
  }
}
