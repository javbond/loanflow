/**
 * Login request model (for Keycloak Resource Owner Password Grant)
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Keycloak OAuth2 Token Response
 * PRD Compliant: Uses Keycloak tokens
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn?: number;
  scope?: string;
}

/**
 * Token info response from /api/v1/auth/token-info
 * Contains user info extracted from Keycloak JWT
 */
export interface TokenInfoResponse {
  subject: string;
  email: string;
  emailVerified?: boolean;
  preferredUsername?: string;
  givenName?: string;
  familyName?: string;
  fullName?: string;
  roles: string[];
  issuedAt?: string;
  expiresAt?: string;
  issuer?: string;
}

/**
 * User info model (derived from TokenInfoResponse for UI compatibility)
 */
export interface UserInfo {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  roles: string[];
}

/**
 * Role types supported by LoanFlow
 */
export type RoleType = 'ADMIN' | 'LOAN_OFFICER' | 'UNDERWRITER' | 'SUPERVISOR' | 'AUDITOR' | 'CUSTOMER';

/**
 * Role display names
 */
export const ROLE_DISPLAY_NAMES: Record<RoleType, string> = {
  ADMIN: 'Administrator',
  LOAN_OFFICER: 'Loan Officer',
  UNDERWRITER: 'Underwriter',
  SUPERVISOR: 'Supervisor',
  AUDITOR: 'Auditor',
  CUSTOMER: 'Customer'
};

/**
 * Keycloak configuration
 */
export interface KeycloakConfig {
  serverUrl: string;
  realm: string;
  clientId: string;
}

/**
 * Default Keycloak configuration
 */
export const KEYCLOAK_CONFIG: KeycloakConfig = {
  serverUrl: 'http://localhost:8180',
  realm: 'loanflow',
  clientId: 'loanflow-web'
};
