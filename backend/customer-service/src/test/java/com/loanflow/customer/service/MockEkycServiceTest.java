package com.loanflow.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.entity.KycVerification;
import com.loanflow.customer.domain.enums.EkycStatus;
import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.customer.repository.CustomerRepository;
import com.loanflow.customer.repository.KycVerificationRepository;
import com.loanflow.customer.service.impl.MockEkycService;
import com.loanflow.dto.response.EkycInitiateResponse;
import com.loanflow.dto.response.EkycVerifyResponse;
import com.loanflow.dto.response.KycStatusResponse;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for MockEkycService (US-029).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MockEkycService Tests")
class MockEkycServiceTest {

    @Mock
    private KycVerificationRepository kycRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CkycService ckycService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MockEkycService service;

    private UUID customerId;
    private Customer customer;
    private static final String AADHAAR = "123456789012";

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = Customer.builder()
                .id(customerId)
                .customerNumber("CUS-2026-000001")
                .firstName("Rahul")
                .lastName("Sharma")
                .aadhaarNumber(AADHAAR)
                .kycStatus(KycStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Initiate OTP")
    class InitiateOtpTests {

        @Test
        @DisplayName("Should initiate OTP successfully for new customer")
        void shouldInitiateOtpSuccessfully() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(Optional.empty());
            when(kycRepository.save(any(KycVerification.class))).thenAnswer(i -> i.getArgument(0));

            EkycInitiateResponse response = service.initiateOtp(customerId, AADHAAR);

            assertThat(response.getStatus()).isEqualTo("OTP_SENT");
            assertThat(response.getTransactionId()).startsWith("TXN-");
            assertThat(response.getMaskedMobile()).startsWith("XXXXXX");
            assertThat(response.getMessage()).contains("OTP sent");

            ArgumentCaptor<KycVerification> captor = ArgumentCaptor.forClass(KycVerification.class);
            verify(kycRepository).save(captor.capture());
            KycVerification saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(EkycStatus.OTP_SENT);
            assertThat(saved.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return ALREADY_VERIFIED if customer already verified")
        void shouldReturnAlreadyVerifiedIfAlreadyDone() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            KycVerification verified = KycVerification.builder()
                    .customerId(customerId)
                    .aadhaarNumber(AADHAAR)
                    .build();
            verified.markOtpSent("TXN-old");
            verified.markVerified("{}");

            when(kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(Optional.of(verified));

            EkycInitiateResponse response = service.initiateOtp(customerId, AADHAAR);

            assertThat(response.getStatus()).isEqualTo("ALREADY_VERIFIED");
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw if customer not found")
        void shouldThrowIfCustomerNotFound() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.initiateOtp(customerId, AADHAAR))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Verify OTP")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should verify successfully with correct OTP (123456)")
        void shouldVerifyWithCorrectOtp() {
            String txnId = "TXN-testverify";
            KycVerification verification = KycVerification.builder()
                    .customerId(customerId)
                    .aadhaarNumber(AADHAAR)
                    .build();
            verification.markOtpSent(txnId);

            when(kycRepository.findByTransactionId(txnId)).thenReturn(Optional.of(verification));
            when(kycRepository.save(any(KycVerification.class))).thenAnswer(i -> i.getArgument(0));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            when(ckycService.submitToRegistry(any())).thenReturn("CKYC-2026-00012345");

            EkycVerifyResponse response = service.verifyOtp(customerId, txnId, "123456");

            assertThat(response.isVerified()).isTrue();
            assertThat(response.getStatus()).isEqualTo("VERIFIED");
            assertThat(response.getEkycData()).isNotNull();
            assertThat(response.getEkycData().getName()).isNotBlank();
            assertThat(response.getCkycNumber()).isEqualTo("CKYC-2026-00012345");

            // Verify customer was updated
            verify(customerRepository).save(any(Customer.class));
            assertThat(customer.getAadhaarVerified()).isTrue();
        }

        @Test
        @DisplayName("Should fail with wrong OTP")
        void shouldFailWithWrongOtp() {
            String txnId = "TXN-testwrong";
            KycVerification verification = KycVerification.builder()
                    .customerId(customerId)
                    .aadhaarNumber(AADHAAR)
                    .build();
            verification.markOtpSent(txnId);

            when(kycRepository.findByTransactionId(txnId)).thenReturn(Optional.of(verification));
            when(kycRepository.save(any(KycVerification.class))).thenAnswer(i -> i.getArgument(0));

            EkycVerifyResponse response = service.verifyOtp(customerId, txnId, "999999");

            assertThat(response.isVerified()).isFalse();
            assertThat(response.getStatus()).isEqualTo("FAILED");
            assertThat(response.getMessage()).contains("Invalid OTP");
            assertThat(response.getEkycData()).isNull();
            assertThat(response.getCkycNumber()).isNull();
        }

        @Test
        @DisplayName("Should throw if transaction not found")
        void shouldThrowIfTransactionNotFound() {
            when(kycRepository.findByTransactionId("TXN-nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyOtp(customerId, "TXN-nonexistent", "123456"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should reject if transaction belongs to different customer")
        void shouldRejectIfDifferentCustomer() {
            String txnId = "TXN-other";
            KycVerification verification = KycVerification.builder()
                    .customerId(UUID.randomUUID()) // Different customer
                    .aadhaarNumber(AADHAAR)
                    .build();
            verification.markOtpSent(txnId);

            when(kycRepository.findByTransactionId(txnId)).thenReturn(Optional.of(verification));

            assertThatThrownBy(() -> service.verifyOtp(customerId, txnId, "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to this customer");
        }
    }

    @Nested
    @DisplayName("Get KYC Status")
    class GetKycStatusTests {

        @Test
        @DisplayName("Should return NOT_INITIATED when no verification exists")
        void shouldReturnNotInitiated() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(Optional.empty());

            KycStatusResponse response = service.getKycStatus(customerId);

            assertThat(response.getCustomerId()).isEqualTo(customerId);
            assertThat(response.getStatus()).isEqualTo("NOT_INITIATED");
            assertThat(response.getAttemptCount()).isZero();
        }

        @Test
        @DisplayName("Should return verified status with e-KYC data")
        void shouldReturnVerifiedStatus() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            KycVerification verification = KycVerification.builder()
                    .customerId(customerId)
                    .aadhaarNumber(AADHAAR)
                    .build();
            verification.markOtpSent("TXN-verified");
            verification.markVerified("{\"name\":\"Rahul Sharma\",\"dateOfBirth\":\"15-05-1990\"}");
            verification.recordCkycSubmission("CKYC-2026-00012345");

            when(kycRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId))
                    .thenReturn(Optional.of(verification));

            KycStatusResponse response = service.getKycStatus(customerId);

            assertThat(response.getStatus()).isEqualTo("VERIFIED");
            assertThat(response.getCkycNumber()).isEqualTo("CKYC-2026-00012345");
            assertThat(response.getEkycData()).isNotNull();
            assertThat(response.getEkycData().getName()).isEqualTo("Rahul Sharma");
            assertThat(response.getMaskedAadhaar()).isEqualTo("XXXX XXXX 9012");
        }
    }
}
