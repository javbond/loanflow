package com.loanflow.customer.domain.enums;

import lombok.Getter;

@Getter
public enum KycVerificationType {
    AADHAAR_EKYC("Aadhaar e-KYC (OTP-based)"),
    CKYC("Central KYC Registry"),
    MANUAL("Manual Document Verification");

    private final String displayName;

    KycVerificationType(String displayName) {
        this.displayName = displayName;
    }
}
