package com.loanflow.customer.service;

import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    // CRUD Operations
    CustomerResponse create(CustomerRequest request);
    CustomerResponse getById(UUID id);
    CustomerResponse getByCustomerNumber(String customerNumber);
    CustomerResponse getByPan(String panNumber);
    CustomerResponse getByMobile(String mobileNumber);
    CustomerResponse getByEmail(String email);
    Page<CustomerResponse> getAll(Pageable pageable);
    CustomerResponse update(UUID id, CustomerRequest request);

    // Search Operations
    Page<CustomerResponse> searchByName(String name, Pageable pageable);
    Page<CustomerResponse> searchByQuery(String query, Pageable pageable);
    Page<CustomerResponse> getByKycStatus(KycStatus kycStatus, Pageable pageable);

    // KYC Operations
    CustomerResponse verifyAadhaar(UUID id);
    CustomerResponse verifyPan(UUID id);
    CustomerResponse updateKycStatus(UUID id);

    // Status Operations
    CustomerResponse deactivate(UUID id, String reason);
    CustomerResponse block(UUID id, String reason);
    CustomerResponse reactivate(UUID id);
}
