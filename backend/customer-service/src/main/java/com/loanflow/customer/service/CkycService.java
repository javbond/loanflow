package com.loanflow.customer.service;

import com.loanflow.customer.domain.entity.KycVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Mock Central KYC Registry (CKYC) submission service (US-029).
 * In production, this would call the actual CERSAI/CKYC API.
 */
@Service
@Slf4j
public class CkycService {

    private static final Random RANDOM = new Random();

    /**
     * Submit verified e-KYC data to Central KYC Registry.
     * Returns a CKYC number upon successful registration.
     *
     * @param verification the verified KYC verification record
     * @return CKYC registration number
     */
    public String submitToRegistry(KycVerification verification) {
        log.info("CKYC: Submitting e-KYC data for customer {} to Central KYC Registry (mock)",
                verification.getCustomerId());

        // Mock: generate a CKYC number
        String ckycNumber = String.format("CKYC-2026-%08d", RANDOM.nextInt(100000000));

        log.info("CKYC: Registration successful — CKYC number: {}", ckycNumber);
        return ckycNumber;
    }
}
