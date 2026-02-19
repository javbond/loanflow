import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';
import {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserInfo,
  UserResponse
} from '../models/auth.model';

const AUTH_API = '/api/v1/auth';
const TOKEN_KEY = 'loanflow_access_token';
const REFRESH_TOKEN_KEY = 'loanflow_refresh_token';
const USER_KEY = 'loanflow_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private currentUserSubject = new BehaviorSubject<UserInfo | null>(this.loadUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  /**
   * Register a new user
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${AUTH_API}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Login with email and password
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${AUTH_API}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Refresh access token
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }

    return this.http.post<AuthResponse>(`${AUTH_API}/refresh`, { refreshToken }).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        this.logout();
        return throwError(() => error);
      })
    );
  }

  /**
   * Get current user from server
   */
  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${AUTH_API}/me`);
  }

  /**
   * Logout user
   */
  logout(): void {
    // Clear storage
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);

    // Update subjects
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);

    // Navigate to login
    this.router.navigate(['/login']);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasValidToken();
  }

  /**
   * Get current user synchronously
   */
  getCurrentUserSync(): UserInfo | null {
    return this.currentUserSubject.value;
  }

  /**
   * Get access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  /**
   * Get refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user?.roles?.includes(role) ?? false;
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.currentUserSubject.value;
    return roles.some(role => user?.roles?.includes(role));
  }

  // ========== Private Methods ==========

  private handleAuthResponse(response: AuthResponse): void {
    // Store tokens
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));

    // Update subjects
    this.currentUserSubject.next(response.user);
    this.isAuthenticatedSubject.next(true);
  }

  private hasValidToken(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;

    // Check if token is expired (basic check)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000; // Convert to milliseconds
      return Date.now() < exp;
    } catch {
      return false;
    }
  }

  private loadUserFromStorage(): UserInfo | null {
    const userJson = localStorage.getItem(USER_KEY);
    if (!userJson) return null;

    try {
      return JSON.parse(userJson);
    } catch {
      return null;
    }
  }

  private handleError(error: any): Observable<never> {
    let message = 'An error occurred';

    if (error.error?.message) {
      message = error.error.message;
    } else if (error.status === 401) {
      message = 'Invalid email or password';
    } else if (error.status === 403) {
      message = 'Access denied';
    } else if (error.status === 0) {
      message = 'Unable to connect to server';
    }

    return throwError(() => new Error(message));
  }
}
