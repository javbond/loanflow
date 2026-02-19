package com.loanflow.customer.controller;

import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.customer.service.CustomerService;
import com.loanflow.dto.common.ApiResponse;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    @Operation(summary = "Create a new customer")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(
            @Parameter(description = "Customer UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping("/number/{customerNumber}")
    @Operation(summary = "Get customer by customer number")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByCustomerNumber(
            @Parameter(description = "Customer number (e.g., CUS-2024-000001)")
            @PathVariable String customerNumber) {
        return ResponseEntity.ok(ApiResponse.success(service.getByCustomerNumber(customerNumber)));
    }

    @GetMapping("/pan/{panNumber}")
    @Operation(summary = "Get customer by PAN number")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByPan(
            @PathVariable String panNumber) {
        return ResponseEntity.ok(ApiResponse.success(service.getByPan(panNumber)));
    }

    @GetMapping("/mobile/{mobileNumber}")
    @Operation(summary = "Get customer by mobile number")
    public ResponseEntity<ApiResponse<CustomerResponse>> getByMobile(
            @PathVariable String mobileNumber) {
        return ResponseEntity.ok(ApiResponse.success(service.getByMobile(mobileNumber)));
    }

    @GetMapping
    @Operation(summary = "List all customers with pagination")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by name, PAN, or mobile number")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.searchByQuery(query, pageable));
    }

    @GetMapping("/kyc-status/{status}")
    @Operation(summary = "Get customers by KYC status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> getByKycStatus(
            @PathVariable KycStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getByKycStatus(status, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.update(id, request),
                "Customer updated successfully"));
    }

    @PostMapping("/{id}/verify-aadhaar")
    @Operation(summary = "Verify customer Aadhaar")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyAadhaar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.verifyAadhaar(id),
                "Aadhaar verified successfully"));
    }

    @PostMapping("/{id}/verify-pan")
    @Operation(summary = "Verify customer PAN")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyPan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.verifyPan(id),
                "PAN verified successfully"));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate customer")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> deactivate(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                service.deactivate(id, reason),
                "Customer deactivated"));
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Block customer")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> block(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                service.block(id, reason),
                "Customer blocked"));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate customer")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> reactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.reactivate(id),
                "Customer reactivated"));
    }
}
