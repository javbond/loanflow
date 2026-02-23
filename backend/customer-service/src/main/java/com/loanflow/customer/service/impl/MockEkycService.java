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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Mock e-KYC service for dev/test environments (US-029).
 * Simulates UIDAI Aadhaar OTP-based verification with realistic response payloads.
 * OTP "123456" succeeds; any other OTP fails.
 */
@Service
@Profile({"dev", "test", "default"})
@RequiredArgsConstructor
@Slf4j
public class MockEkycService implements EkycService {

    private static final String MOCK_OTP = "123456";
    private static final int MAX_OTP_ATTEMPTS = 3;

    private final KycVerificationRepository kycRepository;
    private final CustomerRepository customerRepository;
    private final CkycService ckycService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EkycInitiateResponse initiateOtp(UUID customerId, String aadhaarNumber) {
        log.info("MockEkyc: Initiating OTP for customer {} with Aadhaar ***{}",
                customerId, aadhaarNumber.substring(8));

        // Validate customer exists
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Check for existing active verification
        var existingVerification = kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);

        if (existingVerification.isPresent()) {
            KycVerification existing = existingVerification.get();
            if (existing.isVerified()) {
                return EkycInitiateResponse.builder()
                        .status("ALREADY_VERIFIED")
                        .message("Customer e-KYC is already verified")
                        .build();
            }
            if (existing.isMaxAttemptsExceeded()) {
                return EkycInitiateResponse.builder()
                        .status("MAX_ATTEMPTS_EXCEEDED")
                        .message("Maximum OTP attempts exceeded. Please try again later.")
                        .build();
            }
            // Re-use existing verification if still in OTP_SENT or FAILED state
            if (existing.getStatus() == EkycStatus.OTP_SENT || existing.getStatus() == EkycStatus.FAILED) {
                String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
                existing.markOtpSent(txnId);
                kycRepository.save(existing);

                log.info("MockEkyc: OTP re-sent for transaction {} (attempt {})",
                        txnId, existing.getAttemptCount());

                return EkycInitiateResponse.builder()
                        .transactionId(txnId)
                        .maskedMobile(getMaskedMobile(aadhaarNumber))
                        .status("OTP_SENT")
                        .message("OTP sent to Aadhaar-linked mobile number")
                        .build();
            }
        }

        // Create new verification record
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        KycVerification verification = KycVerification.builder()
                .customerId(customerId)
                .aadhaarNumber(aadhaarNumber)
                .build();
        verification.markOtpSent(txnId);
        kycRepository.save(verification);

        log.info("MockEkyc: New verification created — transaction {}, attempt {}",
                txnId, verification.getAttemptCount());

        return EkycInitiateResponse.builder()
                .transactionId(txnId)
                .maskedMobile(getMaskedMobile(aadhaarNumber))
                .status("OTP_SENT")
                .message("OTP sent to Aadhaar-linked mobile number")
                .build();
    }

    @Override
    @Transactional
    public EkycVerifyResponse verifyOtp(UUID customerId, String transactionId, String otp) {
        log.info("MockEkyc: Verifying OTP for customer {}, transaction {}", customerId, transactionId);

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
                    .message("Verification is not in OTP_SENT state. Current status: " + verification.getStatus())
                    .build();
        }

        // Mock OTP validation
        if (MOCK_OTP.equals(otp)) {
            // Success — populate mock e-KYC data
            EkycData ekycData = buildMockEkycData(verification.getAadhaarNumber());
            String ekycJson = serializeEkycData(ekycData);

            verification.markVerified(ekycJson);

            // Submit to CKYC registry
            String ckycNumber = ckycService.submitToRegistry(verification);
            verification.recordCkycSubmission(ckycNumber);
            kycRepository.save(verification);

            // Update customer verification status
            var customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
            customer.verifyAadhaar();
            customer.updateKycStatus();
            customerRepository.save(customer);

            log.info("MockEkyc: Verification SUCCESS for customer {}, CKYC: {}", customerId, ckycNumber);

            return EkycVerifyResponse.builder()
                    .verified(true)
                    .transactionId(transactionId)
                    .status("VERIFIED")
                    .message("e-KYC verification successful")
                    .ekycData(ekycData)
                    .ckycNumber(ckycNumber)
                    .build();
        } else {
            // Failure — wrong OTP
            verification.markFailed("Invalid OTP");
            kycRepository.save(verification);

            log.warn("MockEkyc: Verification FAILED for customer {} — invalid OTP (attempt {})",
                    customerId, verification.getAttemptCount());

            return EkycVerifyResponse.builder()
                    .verified(false)
                    .transactionId(transactionId)
                    .status("FAILED")
                    .message("Invalid OTP. Attempts used: " + verification.getAttemptCount() + "/" + MAX_OTP_ATTEMPTS)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(UUID customerId) {
        log.debug("MockEkyc: Getting KYC status for customer {}", customerId);

        // Validate customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        var latestVerification = kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);

        if (latestVerification.isEmpty()) {
            return KycStatusResponse.builder()
                    .customerId(customerId)
                    .status("NOT_INITIATED")
                    .attemptCount(0)
                    .message("e-KYC verification has not been initiated")
                    .build();
        }

        KycVerification verification = latestVerification.get();
        EkycData ekycData = null;

        if (verification.isVerified() && verification.getEkycData() != null) {
            ekycData = deserializeEkycData(verification.getEkycData());
        }

        return KycStatusResponse.builder()
                .customerId(customerId)
                .status(verification.getStatus().name())
                .verifiedAt(verification.getVerifiedAt())
                .ckycNumber(verification.getCkycNumber())
                .ekycData(ekycData)
                .attemptCount(verification.getAttemptCount())
                .maskedAadhaar(verification.getMaskedAadhaar())
                .message(getStatusMessage(verification))
                .build();
    }

    // ==================== Private Helpers ====================

    private EkycData buildMockEkycData(String aadhaarNumber) {
        // Deterministic mock data based on Aadhaar hash
        int hash = Math.abs(aadhaarNumber.hashCode());
        String[] names = {"Rahul Sharma", "Priya Patel", "Amit Kumar", "Sneha Gupta", "Rajesh Singh"};
        String[] states = {"Maharashtra", "Gujarat", "Karnataka", "Tamil Nadu", "Delhi"};
        String[] districts = {"Mumbai", "Ahmedabad", "Bangalore", "Chennai", "New Delhi"};
        String[] pins = {"400001", "380001", "560001", "600001", "110001"};

        int idx = hash % names.length;

        return EkycData.builder()
                .name(names[idx])
                .dateOfBirth("15-05-1990")
                .gender(hash % 2 == 0 ? "MALE" : "FEMALE")
                .address("42, Sector " + (hash % 50 + 1) + ", " + districts[idx] + ", " + states[idx])
                .pinCode(pins[idx])
                .state(states[idx])
                .district(districts[idx])
                .photo(null)  // No mock photo
                .build();
    }

    private String getMaskedMobile(String aadhaarNumber) {
        // Deterministic mock mobile from Aadhaar hash
        int hash = Math.abs(aadhaarNumber.hashCode());
        String lastFour = String.format("%04d", hash % 10000);
        return "XXXXXX" + lastFour;
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

    private String getStatusMessage(KycVerification verification) {
        return switch (verification.getStatus()) {
            case PENDING -> "Verification is pending";
            case OTP_SENT -> "OTP has been sent. Awaiting verification.";
            case VERIFIED -> "e-KYC verification is complete";
            case FAILED -> "Verification failed: " + verification.getFailureReason();
            case EXPIRED -> "Verification has expired. Please re-initiate.";
        };
    }
}
