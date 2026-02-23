package com.loanflow.customer.domain.entity;

import com.loanflow.customer.domain.enums.EkycStatus;
import com.loanflow.customer.domain.enums.KycVerificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_verifications", schema = "identity",
        indexes = {
                @Index(name = "idx_kyc_customer_id", columnList = "customer_id"),
                @Index(name = "idx_kyc_status", columnList = "status"),
                @Index(name = "idx_kyc_transaction_id", columnList = "transaction_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerification {

    private static final int MAX_OTP_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "aadhaar_number", nullable = false, length = 12)
    private String aadhaarNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    @Builder.Default
    private KycVerificationType verificationType = KycVerificationType.AADHAAR_EKYC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EkycStatus status = EkycStatus.PENDING;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "otp_sent_at")
    private Instant otpSentAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "ekyc_data", columnDefinition = "jsonb")
    private String ekycData;

    @Column(name = "ckyc_number", length = 20)
    private String ckycNumber;

    @Column(name = "ckyc_submitted_at")
    private Instant ckycSubmittedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ==================== Business Methods ====================

    /**
     * Mark OTP as sent — transitions from PENDING to OTP_SENT
     */
    public void markOtpSent(String transactionId) {
        if (this.status != EkycStatus.PENDING && this.status != EkycStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot send OTP in status: " + this.status + ". Must be PENDING or FAILED.");
        }
        this.transactionId = transactionId;
        this.status = EkycStatus.OTP_SENT;
        this.otpSentAt = Instant.now();
        this.attemptCount++;
    }

    /**
     * Mark verification as successful — transitions from OTP_SENT to VERIFIED
     */
    public void markVerified(String ekycData) {
        if (this.status != EkycStatus.OTP_SENT) {
            throw new IllegalStateException(
                    "Cannot verify in status: " + this.status + ". Must be OTP_SENT.");
        }
        this.status = EkycStatus.VERIFIED;
        this.verifiedAt = Instant.now();
        this.ekycData = ekycData;
    }

    /**
     * Mark verification as failed — transitions from OTP_SENT to FAILED
     */
    public void markFailed(String reason) {
        if (this.status != EkycStatus.OTP_SENT) {
            throw new IllegalStateException(
                    "Cannot fail in status: " + this.status + ". Must be OTP_SENT.");
        }
        this.status = EkycStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Mark as expired
     */
    public void markExpired() {
        this.status = EkycStatus.EXPIRED;
        this.expiredAt = Instant.now();
    }

    /**
     * Record CKYC submission
     */
    public void recordCkycSubmission(String ckycNumber) {
        this.ckycNumber = ckycNumber;
        this.ckycSubmittedAt = Instant.now();
    }

    /**
     * Check if max OTP attempts exceeded
     */
    public boolean isMaxAttemptsExceeded() {
        return this.attemptCount >= MAX_OTP_ATTEMPTS;
    }

    /**
     * Check if verification is complete (terminal success state)
     */
    public boolean isVerified() {
        return this.status == EkycStatus.VERIFIED;
    }

    /**
     * Get masked Aadhaar (XXXX XXXX 9012)
     */
    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() != 12) {
            return null;
        }
        return "XXXX XXXX " + aadhaarNumber.substring(8);
    }
}
