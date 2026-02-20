import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TaskResponse, CompleteTaskRequest, SpringPage, ApiResponse } from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = '/api/v1/tasks';

  constructor(private http: HttpClient) {}

  /** Get unclaimed tasks for user's roles (candidateGroups) */
  getInbox(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<SpringPage<TaskResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<SpringPage<TaskResponse>>(`${this.apiUrl}/inbox`, { params });
  }

  /** Get tasks claimed by current user */
  getMyTasks(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<SpringPage<TaskResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<SpringPage<TaskResponse>>(`${this.apiUrl}/my-tasks`, { params });
  }

  /** Get single task by ID */
  getTask(taskId: string): Observable<ApiResponse<TaskResponse>> {
    return this.http.get<ApiResponse<TaskResponse>>(`${this.apiUrl}/${taskId}`);
  }

  /** Get active task for a specific loan application */
  getTaskForApplication(applicationId: string): Observable<ApiResponse<TaskResponse>> {
    return this.http.get<ApiResponse<TaskResponse>>(`${this.apiUrl}/application/${applicationId}`);
  }

  /** Claim a task */
  claimTask(taskId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${taskId}/claim`, {});
  }

  /** Release (unclaim) a task */
  unclaimTask(taskId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${taskId}/unclaim`, {});
  }

  /** Complete a task with a decision */
  completeTask(taskId: string, request: CompleteTaskRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${taskId}/complete`, request);
  }
}
