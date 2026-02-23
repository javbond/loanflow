package com.loanflow.customer.domain;

import com.loanflow.customer.domain.entity.KycVerification;
import com.loanflow.customer.domain.enums.EkycStatus;
import com.loanflow.customer.domain.enums.KycVerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD unit tests for KycVerification entity (US-029).
 */
@DisplayName("KycVerification Entity Tests")
class KycVerificationTest {

    private KycVerification verification;

    @BeforeEach
    void setUp() {
        verification = KycVerification.builder()
                .customerId(UUID.randomUUID())
                .aadhaarNumber("123456789012")
                .build();
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition from PENDING to OTP_SENT")
        void shouldTransitionFromPendingToOtpSent() {
            assertThat(verification.getStatus()).isEqualTo(EkycStatus.PENDING);

            verification.markOtpSent("TXN-12345678");

            assertThat(verification.getStatus()).isEqualTo(EkycStatus.OTP_SENT);
            assertThat(verification.getTransactionId()).isEqualTo("TXN-12345678");
            assertThat(verification.getOtpSentAt()).isNotNull();
            assertThat(verification.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should transition from OTP_SENT to VERIFIED")
        void shouldTransitionFromOtpSentToVerified() {
            verification.markOtpSent("TXN-12345678");

            verification.markVerified("{\"name\": \"Rahul Sharma\"}");

            assertThat(verification.getStatus()).isEqualTo(EkycStatus.VERIFIED);
            assertThat(verification.getVerifiedAt()).isNotNull();
            assertThat(verification.getEkycData()).contains("Rahul Sharma");
        }

        @Test
        @DisplayName("Should transition from OTP_SENT to FAILED")
        void shouldTransitionFromOtpSentToFailed() {
            verification.markOtpSent("TXN-12345678");

            verification.markFailed("Invalid OTP");

            assertThat(verification.getStatus()).isEqualTo(EkycStatus.FAILED);
            assertThat(verification.getFailureReason()).isEqualTo("Invalid OTP");
        }

        @Test
        @DisplayName("Should allow re-initiation from FAILED state")
        void shouldAllowReInitiationFromFailedState() {
            verification.markOtpSent("TXN-first");
            verification.markFailed("Wrong OTP");

            // Re-initiate should work from FAILED state
            assertThatCode(() -> verification.markOtpSent("TXN-second"))
                    .doesNotThrowAnyException();

            assertThat(verification.getStatus()).isEqualTo(EkycStatus.OTP_SENT);
            assertThat(verification.getTransactionId()).isEqualTo("TXN-second");
            assertThat(verification.getAttemptCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not allow OTP send from VERIFIED state")
        void shouldNotAllowOtpSendFromVerifiedState() {
            verification.markOtpSent("TXN-12345678");
            verification.markVerified("{}");

            assertThatThrownBy(() -> verification.markOtpSent("TXN-new"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot send OTP in status: VERIFIED");
        }

        @Test
        @DisplayName("Should not allow verify from PENDING state")
        void shouldNotAllowVerifyFromPendingState() {
            assertThatThrownBy(() -> verification.markVerified("{}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot verify in status: PENDING");
        }

        @Test
        @DisplayName("Should not allow fail from PENDING state")
        void shouldNotAllowFailFromPendingState() {
            assertThatThrownBy(() -> verification.markFailed("error"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot fail in status: PENDING");
        }
    }

    @Nested
    @DisplayName("Attempt Tracking")
    class AttemptTrackingTests {

        @Test
        @DisplayName("Should track OTP attempts and detect max exceeded")
        void shouldTrackOtpAttempts() {
            // Attempt 1
            verification.markOtpSent("TXN-1");
            assertThat(verification.getAttemptCount()).isEqualTo(1);
            assertThat(verification.isMaxAttemptsExceeded()).isFalse();

            // Attempt 2
            verification.markFailed("wrong");
            verification.markOtpSent("TXN-2");
            assertThat(verification.getAttemptCount()).isEqualTo(2);
            assertThat(verification.isMaxAttemptsExceeded()).isFalse();

            // Attempt 3
            verification.markFailed("wrong");
            verification.markOtpSent("TXN-3");
            assertThat(verification.getAttemptCount()).isEqualTo(3);
            assertThat(verification.isMaxAttemptsExceeded()).isTrue();
        }
    }

    @Nested
    @DisplayName("Business Methods")
    class BusinessMethodTests {

        @Test
        @DisplayName("Should correctly identify verified state")
        void shouldIdentifyVerifiedState() {
            assertThat(verification.isVerified()).isFalse();

            verification.markOtpSent("TXN-1");
            assertThat(verification.isVerified()).isFalse();

            verification.markVerified("{}");
            assertThat(verification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("Should mask Aadhaar number correctly")
        void shouldMaskAadhaarNumber() {
            assertThat(verification.getMaskedAadhaar()).isEqualTo("XXXX XXXX 9012");
        }

        @Test
        @DisplayName("Should record CKYC submission")
        void shouldRecordCkycSubmission() {
            verification.markOtpSent("TXN-1");
            verification.markVerified("{}");

            verification.recordCkycSubmission("CKYC-2026-00012345");

            assertThat(verification.getCkycNumber()).isEqualTo("CKYC-2026-00012345");
            assertThat(verification.getCkycSubmittedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should default to AADHAAR_EKYC verification type")
        void shouldDefaultToAadhaarEkyc() {
            assertThat(verification.getVerificationType()).isEqualTo(KycVerificationType.AADHAAR_EKYC);
        }

        @Test
        @DisplayName("Should mark as expired")
        void shouldMarkAsExpired() {
            verification.markExpired();

            assertThat(verification.getStatus()).isEqualTo(EkycStatus.EXPIRED);
            assertThat(verification.getExpiredAt()).isNotNull();
        }
    }
}
