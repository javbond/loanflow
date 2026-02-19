import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap, catchError, throwError, map } from 'rxjs';
import {
  LoginRequest,
  AuthResponse,
  TokenInfoResponse,
  UserInfo,
  KEYCLOAK_CONFIG
} from '../models/auth.model';

const AUTH_API = '/api/v1/auth';
const TOKEN_KEY = 'loanflow_access_token';
const REFRESH_TOKEN_KEY = 'loanflow_refresh_token';
const USER_KEY = 'loanflow_user';

/**
 * Authentication Service - Keycloak OAuth2/OIDC Integration
 * PRD Compliant: Uses Keycloak for authentication
 */
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
   * Login via Keycloak OAuth2 token endpoint
   * Uses Resource Owner Password Credentials grant for form-based login
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${AUTH_API}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(this.handleError)
    );
  }

  /**
   * Refresh access token via Keycloak
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
   * Get current user info from JWT token
   */
  getCurrentUser(): Observable<UserInfo> {
    return this.http.get<TokenInfoResponse>(`${AUTH_API}/me`).pipe(
      map(tokenInfo => this.tokenInfoToUserInfo(tokenInfo)),
      tap(user => {
        this.currentUserSubject.next(user);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
      })
    );
  }

  /**
   * Logout user - invalidates Keycloak session
   */
  logout(): void {
    const refreshToken = this.getRefreshToken();

    // Call backend to invalidate Keycloak session
    if (refreshToken) {
      this.http.post<{ message: string; keycloakLogoutUrl: string }>(
        `${AUTH_API}/logout`,
        { refreshToken }
      ).subscribe({
        next: (response) => {
          console.log('Logged out from Keycloak');
          // Optionally redirect to Keycloak logout page for SSO logout
          // window.location.href = response.keycloakLogoutUrl;
        },
        error: (err) => console.warn('Logout request failed', err)
      });
    }

    // Clear local storage
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
   * Redirect to Keycloak login page (for Authorization Code Flow)
   * Alternative to ROPC grant - more secure for production
   */
  loginWithKeycloak(): void {
    const redirectUri = encodeURIComponent(window.location.origin + '/auth/callback');
    const keycloakAuthUrl = `${KEYCLOAK_CONFIG.serverUrl}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/auth`;

    const params = new URLSearchParams({
      client_id: KEYCLOAK_CONFIG.clientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: 'openid profile email',
      code_challenge_method: 'S256',
      // In production, generate a proper PKCE code challenge
    });

    window.location.href = `${keycloakAuthUrl}?${params.toString()}`;
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

    // Update authentication state
    this.isAuthenticatedSubject.next(true);

    // Extract user info from JWT and store
    const userInfo = this.extractUserInfoFromJwt(response.accessToken);
    if (userInfo) {
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
      this.currentUserSubject.next(userInfo);
    }
  }

  /**
   * Extract user info from Keycloak JWT token
   */
  private extractUserInfoFromJwt(token: string): UserInfo | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));

      // Extract roles from Keycloak JWT
      let roles: string[] = [];
      if (payload.realm_access?.roles) {
        roles = payload.realm_access.roles.filter((r: string) => !r.startsWith('default-roles-'));
      }
      if (payload.roles) {
        const customRoles = payload.roles.filter((r: string) => !r.startsWith('default-roles-'));
        roles = [...new Set([...roles, ...customRoles])];
      }

      return {
        id: payload.sub,
        email: payload.email || payload.preferred_username,
        firstName: payload.given_name || '',
        lastName: payload.family_name || '',
        fullName: payload.name || `${payload.given_name || ''} ${payload.family_name || ''}`.trim(),
        roles: roles
      };
    } catch (e) {
      console.error('Failed to extract user info from JWT', e);
      return null;
    }
  }

  private tokenInfoToUserInfo(tokenInfo: TokenInfoResponse): UserInfo {
    return {
      id: tokenInfo.subject,
      email: tokenInfo.email,
      firstName: tokenInfo.givenName || '',
      lastName: tokenInfo.familyName || '',
      fullName: tokenInfo.fullName || `${tokenInfo.givenName || ''} ${tokenInfo.familyName || ''}`.trim(),
      roles: tokenInfo.roles || []
    };
  }

  private hasValidToken(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;

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
