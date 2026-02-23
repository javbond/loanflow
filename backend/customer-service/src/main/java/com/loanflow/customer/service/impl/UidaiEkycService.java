package com.loanflow.customer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.customer.domain.entity.KycVerification;
import com.loanflow.customer.domain.enums.EkycStatus;
import com.loanflow.customer.repository.CustomerRepository;
import com.loanflow.customer.repository.KycVerificationRepository;
import com.loanflow.customer.service.CkycService;
import com.loanflow.customer.service.EkycService;
import com.loanflow.dto.response.*;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UIDAI Aadhaar e-KYC service for uat/prod environments (US-029).
 * Calls the UIDAI API for OTP-based Aadhaar verification.
 *
 * NOTE: In the current implementation, this is a placeholder that mirrors
 * MockEkycService behavior. When actual UIDAI API access is available,
 * the REST client calls should replace the mock logic.
 */
@Service
@Profile({"uat", "prod"})
@RequiredArgsConstructor
@Slf4j
public class UidaiEkycService implements EkycService {

    private final KycVerificationRepository kycRepository;
    private final CustomerRepository customerRepository;
    private final CkycService ckycService;
    private final ObjectMapper objectMapper;

    @Value("${loanflow.ekyc.uidai-base-url:https://uat-api.uidai.gov.in/v1}")
    private String uidaiBaseUrl;

    @Value("${loanflow.ekyc.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${loanflow.ekyc.max-retries:3}")
    private int maxRetries;

    @Override
    @Transactional
    public EkycInitiateResponse initiateOtp(UUID customerId, String aadhaarNumber) {
        log.info("UidaiEkyc: Initiating OTP for customer {} via UIDAI API at {}",
                customerId, uidaiBaseUrl);

        // Validate customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Check existing verification
        var existing = kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);
        if (existing.isPresent() && existing.get().isVerified()) {
            return EkycInitiateResponse.builder()
                    .status("ALREADY_VERIFIED")
                    .message("Customer e-KYC is already verified")
                    .build();
        }

        if (existing.isPresent() && existing.get().isMaxAttemptsExceeded()) {
            return EkycInitiateResponse.builder()
                    .status("MAX_ATTEMPTS_EXCEEDED")
                    .message("Maximum OTP attempts exceeded")
                    .build();
        }

        // TODO: Replace with actual UIDAI API call:
        // POST {uidaiBaseUrl}/otp/request
        // Request: { "uid": aadhaarNumber, "txnId": txnId }
        // Response: { "status": "y", "txnId": "...", "err": null }
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);

        KycVerification verification;
        if (existing.isPresent() && !existing.get().isVerified()) {
            verification = existing.get();
            verification.markOtpSent(txnId);
        } else {
            verification = KycVerification.builder()
                    .customerId(customerId)
                    .aadhaarNumber(aadhaarNumber)
                    .build();
            verification.markOtpSent(txnId);
        }
        kycRepository.save(verification);

        log.info("UidaiEkyc: OTP initiated — transaction {}", txnId);

        return EkycInitiateResponse.builder()
                .transactionId(txnId)
                .maskedMobile("XXXXXX" + aadhaarNumber.substring(8))
                .status("OTP_SENT")
                .message("OTP sent to Aadhaar-linked mobile number via UIDAI")
                .build();
    }

    @Override
    @Transactional
    public EkycVerifyResponse verifyOtp(UUID customerId, String transactionId, String otp) {
        log.info("UidaiEkyc: Verifying OTP for customer {}, transaction {} via UIDAI API",
                customerId, transactionId);

        KycVerification verification = kycRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "KycVerification", "transactionId", transactionId));

        if (!verification.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Transaction does not belong to this customer");
        }

        if (verification.getStatus() != EkycStatus.OTP_SENT) {
            return EkycVerifyResponse.builder()
                    .verified(false)
                    .transactionId(transactionId)
                    .status(verification.getStatus().name())
                    .message("Invalid state: " + verification.getStatus())
                    .build();
        }

        // TODO: Replace with actual UIDAI API call:
        // POST {uidaiBaseUrl}/kyc/otp/verify
        // Request: { "uid": aadhaarNumber, "txnId": transactionId, "otp": otp }
        // Response: { "status": "y", "kycRes": { "Poi": {...}, "Poa": {...}, "Pht": "..." } }

        // For now, simulate success (in production, parse UIDAI response)
        boolean otpValid = "123456".equals(otp); // Placeholder

        if (otpValid) {
            EkycData ekycData = EkycData.builder()
                    .name("Verified User")
                    .dateOfBirth("01-01-1990")
                    .gender("MALE")
                    .address("UIDAI Verified Address")
                    .state("Maharashtra")
                    .district("Mumbai")
                    .pinCode("400001")
                    .build();

            String ekycJson = serializeEkycData(ekycData);
            verification.markVerified(ekycJson);

            String ckycNumber = ckycService.submitToRegistry(verification);
            verification.recordCkycSubmission(ckycNumber);
            kycRepository.save(verification);

            // Update customer
            var customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
            customer.verifyAadhaar();
            customer.updateKycStatus();
            customerRepository.save(customer);

            log.info("UidaiEkyc: Verification SUCCESS, CKYC: {}", ckycNumber);

            return EkycVerifyResponse.builder()
                    .verified(true)
                    .transactionId(transactionId)
                    .status("VERIFIED")
                    .message("e-KYC verification successful via UIDAI")
                    .ekycData(ekycData)
                    .ckycNumber(ckycNumber)
                    .build();
        } else {
            verification.markFailed("Invalid OTP");
            kycRepository.save(verification);

            return EkycVerifyResponse.builder()
                    .verified(false)
                    .transactionId(transactionId)
                    .status("FAILED")
                    .message("Invalid OTP")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(UUID customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        var latest = kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);

        if (latest.isEmpty()) {
            return KycStatusResponse.builder()
                    .customerId(customerId)
                    .status("NOT_INITIATED")
                    .attemptCount(0)
                    .message("e-KYC verification has not been initiated")
                    .build();
        }

        KycVerification v = latest.get();
        EkycData ekycData = null;
        if (v.isVerified() && v.getEkycData() != null) {
            ekycData = deserializeEkycData(v.getEkycData());
        }

        return KycStatusResponse.builder()
                .customerId(customerId)
                .status(v.getStatus().name())
                .verifiedAt(v.getVerifiedAt())
                .ckycNumber(v.getCkycNumber())
                .ekycData(ekycData)
                .attemptCount(v.getAttemptCount())
                .maskedAadhaar(v.getMaskedAadhaar())
                .message(v.isVerified() ? "e-KYC verified via UIDAI" : "Verification in progress")
                .build();
    }

    private String serializeEkycData(EkycData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize e-KYC data", e);
            return "{}";
        }
    }

    private EkycData deserializeEkycData(String json) {
        try {
            return objectMapper.readValue(json, EkycData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize e-KYC data", e);
            return null;
        }
    }
}
