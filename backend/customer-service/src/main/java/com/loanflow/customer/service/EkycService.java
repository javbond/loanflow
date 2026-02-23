package com.loanflow.customer.service;

import com.loanflow.dto.response.EkycInitiateResponse;
import com.loanflow.dto.response.EkycVerifyResponse;
import com.loanflow.dto.response.KycStatusResponse;

import java.util.UUID;

/**
 * Service interface for UIDAI Aadhaar e-KYC verification (US-029).
 * Implementations: MockEkycService (dev/test), UidaiEkycService (uat/prod).
 */
public interface EkycService {

    /**
     * Initiate OTP-based e-KYC verification by sending OTP to Aadhaar-linked mobile.
     *
     * @param customerId   the customer UUID
     * @param aadhaarNumber 12-digit Aadhaar number
     * @return initiation response with transaction ID and masked mobile
     */
    EkycInitiateResponse initiateOtp(UUID customerId, String aadhaarNumber);

    /**
     * Verify OTP and retrieve e-KYC demographic data from UIDAI.
     *
     * @param customerId    the customer UUID
     * @param transactionId the transaction ID from initiation
     * @param otp           the 6-digit OTP entered by the user
     * @return verification response with e-KYC data if successful
     */
    EkycVerifyResponse verifyOtp(UUID customerId, String transactionId, String otp);

    /**
     * Get current KYC verification status for a customer.
     *
     * @param customerId the customer UUID
     * @return KYC status response
     */
    KycStatusResponse getKycStatus(UUID customerId);
}
