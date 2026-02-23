package com.loanflow.customer.domain.enums;

import lombok.Getter;

@Getter
public enum EkycStatus {
    PENDING("Pending"),
    OTP_SENT("OTP Sent"),
    VERIFIED("Verified"),
    FAILED("Failed"),
    EXPIRED("Expired");

    private final String displayName;

    EkycStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isTerminal() {
        return this == VERIFIED || this == FAILED || this == EXPIRED;
    }

    public boolean canRetry() {
        return this == FAILED || this == EXPIRED;
    }
}
