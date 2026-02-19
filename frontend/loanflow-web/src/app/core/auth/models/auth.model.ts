/**
 * Login request model
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Register request model
 */
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

/**
 * User info model
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
 * Authentication response model
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

/**
 * User response model
 */
export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone?: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

/**
 * Role types
 */
export type RoleType = 'ADMIN' | 'LOAN_OFFICER' | 'UNDERWRITER' | 'SENIOR_UNDERWRITER' | 'CUSTOMER';

/**
 * Role display names
 */
export const ROLE_DISPLAY_NAMES: Record<RoleType, string> = {
  ADMIN: 'Administrator',
  LOAN_OFFICER: 'Loan Officer',
  UNDERWRITER: 'Underwriter',
  SENIOR_UNDERWRITER: 'Senior Underwriter',
  CUSTOMER: 'Customer'
};
