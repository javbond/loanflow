package com.loanflow.customer.controller;

import com.loanflow.customer.service.EkycService;
import com.loanflow.dto.request.EkycInitiateRequest;
import com.loanflow.dto.request.EkycVerifyRequest;
import com.loanflow.dto.response.ApiResponse;
import com.loanflow.dto.response.EkycInitiateResponse;
import com.loanflow.dto.response.EkycVerifyResponse;
import com.loanflow.dto.response.KycStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for UIDAI Aadhaar e-KYC verification (US-029).
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/ekyc")
@Tag(name = "e-KYC", description = "UIDAI Aadhaar e-KYC Verification APIs")
@RequiredArgsConstructor
@Slf4j
public class EkycController {

    private final EkycService ekycService;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Initiate e-KYC OTP",
            description = "Send OTP to Aadhaar-linked mobile number for e-KYC verification")
    public ResponseEntity<ApiResponse<EkycInitiateResponse>> initiateEkyc(
            @PathVariable UUID customerId,
            @Valid @RequestBody EkycInitiateRequest request) {

        log.info("e-KYC initiate request for customer {}", customerId);
        EkycInitiateResponse response = ekycService.initiateOtp(customerId, request.getAadhaarNumber());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("e-KYC OTP initiation processed", response));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Verify e-KYC OTP",
            description = "Verify OTP and retrieve e-KYC demographic data from UIDAI")
    public ResponseEntity<ApiResponse<EkycVerifyResponse>> verifyOtp(
            @PathVariable UUID customerId,
            @Valid @RequestBody EkycVerifyRequest request) {

        log.info("e-KYC verify request for customer {}, transaction {}", customerId, request.getTransactionId());
        EkycVerifyResponse response = ekycService.verifyOtp(
                customerId, request.getTransactionId(), request.getOtp());

        return ResponseEntity.ok(ApiResponse.success(
                response.isVerified() ? "e-KYC verification successful" : "e-KYC verification failed",
                response));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get KYC status",
            description = "Get current e-KYC verification status for a customer")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getKycStatus(
            @PathVariable UUID customerId) {

        log.debug("KYC status request for customer {}", customerId);
        KycStatusResponse response = ekycService.getKycStatus(customerId);

        return ResponseEntity.ok(ApiResponse.success("KYC status retrieved", response));
    }
}
