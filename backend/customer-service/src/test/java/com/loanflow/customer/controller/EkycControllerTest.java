package com.loanflow.customer.controller;

import com.loanflow.customer.service.EkycService;
import com.loanflow.dto.response.*;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for EkycController (US-029).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EkycController Tests")
class EkycControllerTest {

    @Mock
    private EkycService ekycService;

    @InjectMocks
    private EkycController controller;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("POST /initiate")
    class InitiateTests {

        @Test
        @DisplayName("Should initiate e-KYC and return OTP_SENT")
        void shouldInitiateEkyc() {
            var expectedResponse = EkycInitiateResponse.builder()
                    .transactionId("TXN-12345678")
                    .maskedMobile("XXXXXX7890")
                    .status("OTP_SENT")
                    .message("OTP sent to Aadhaar-linked mobile number")
                    .build();

            when(ekycService.initiateOtp(customerId, "123456789012")).thenReturn(expectedResponse);

            var request = new com.loanflow.dto.request.EkycInitiateRequest("123456789012");
            var result = controller.initiateEkyc(customerId, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData().getTransactionId()).isEqualTo("TXN-12345678");
            assertThat(result.getBody().getData().getStatus()).isEqualTo("OTP_SENT");
        }

        @Test
        @DisplayName("Should propagate exception for non-existent customer")
        void shouldPropagateExceptionForMissingCustomer() {
            when(ekycService.initiateOtp(eq(customerId), anyString()))
                    .thenThrow(new ResourceNotFoundException("Customer", "id", customerId));

            assertThatThrownBy(() -> controller.initiateEkyc(customerId,
                    new com.loanflow.dto.request.EkycInitiateRequest("123456789012")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("POST /verify")
    class VerifyTests {

        @Test
        @DisplayName("Should verify OTP and return VERIFIED with e-KYC data")
        void shouldVerifyOtp() {
            var ekycData = EkycData.builder()
                    .name("Rahul Sharma")
                    .dateOfBirth("15-05-1990")
                    .gender("MALE")
                    .address("42, Sector 15, Mumbai, Maharashtra")
                    .build();

            var expectedResponse = EkycVerifyResponse.builder()
                    .verified(true)
                    .transactionId("TXN-12345678")
                    .status("VERIFIED")
                    .message("e-KYC verification successful")
                    .ekycData(ekycData)
                    .ckycNumber("CKYC-2026-00012345")
                    .build();

            when(ekycService.verifyOtp(customerId, "TXN-12345678", "123456"))
                    .thenReturn(expectedResponse);

            var request = new com.loanflow.dto.request.EkycVerifyRequest("TXN-12345678", "123456");
            var result = controller.verifyOtp(customerId, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().isVerified()).isTrue();
            assertThat(result.getBody().getData().getEkycData().getName()).isEqualTo("Rahul Sharma");
            assertThat(result.getBody().getData().getCkycNumber()).isEqualTo("CKYC-2026-00012345");
        }
    }

    @Nested
    @DisplayName("GET /status")
    class StatusTests {

        @Test
        @DisplayName("Should return KYC status for customer")
        void shouldReturnKycStatus() {
            var expectedResponse = KycStatusResponse.builder()
                    .customerId(customerId)
                    .status("VERIFIED")
                    .verifiedAt(Instant.now())
                    .ckycNumber("CKYC-2026-00012345")
                    .attemptCount(1)
                    .maskedAadhaar("XXXX XXXX 9012")
                    .message("e-KYC verification is complete")
                    .build();

            when(ekycService.getKycStatus(customerId)).thenReturn(expectedResponse);

            var result = controller.getKycStatus(customerId);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getStatus()).isEqualTo("VERIFIED");
            assertThat(result.getBody().getData().getCkycNumber()).isEqualTo("CKYC-2026-00012345");
            assertThat(result.getBody().getData().getMaskedAadhaar()).isEqualTo("XXXX XXXX 9012");
        }
    }
}
