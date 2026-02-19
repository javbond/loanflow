export interface Customer {
  id?: string;
  customerNumber?: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  email: string;
  mobileNumber: string;
  dateOfBirth: string;
  age?: number;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  panNumber: string;
  aadhaarNumber: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'PENDING_VERIFICATION';
  kycStatus?: 'PENDING' | 'PARTIAL' | 'VERIFIED' | 'REJECTED' | 'EXPIRED';
  aadhaarVerified?: boolean;
  panVerified?: boolean;
  currentAddress?: Address;
  permanentAddress?: Address;
  createdAt?: string;
  updatedAt?: string;
}

export interface Address {
  addressLine1: string;
  addressLine2?: string;
  landmark?: string;
  city: string;
  state: string;
  pinCode: string;
  country: string;
  ownershipType?: 'OWNED' | 'RENTED' | 'COMPANY_PROVIDED';
  yearsAtAddress?: number;
  fullAddress?: string;
}

export interface CustomerRequest {
  firstName: string;
  lastName: string;
  email: string;
  mobileNumber: string;
  dateOfBirth: string;
  gender: string;
  panNumber: string;
  aadhaarNumber: string;
  currentAddress?: Address;
  permanentAddress?: Address;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
