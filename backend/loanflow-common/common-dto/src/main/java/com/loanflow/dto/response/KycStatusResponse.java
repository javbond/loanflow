package com.loanflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "KYC verification status response")
public class KycStatusResponse {

    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Current KYC verification status", example = "VERIFIED")
    private String status;

    @Schema(description = "Timestamp when verification was completed")
    private Instant verifiedAt;

    @Schema(description = "CKYC registry number", example = "CKYC-2026-00012345")
    private String ckycNumber;

    @Schema(description = "e-KYC demographic data (available only if verified)")
    private EkycData ekycData;

    @Schema(description = "Number of OTP verification attempts", example = "1")
    private int attemptCount;

    @Schema(description = "Masked Aadhaar number", example = "XXXX XXXX 9012")
    private String maskedAadhaar;

    @Schema(description = "Descriptive message", example = "KYC verification is complete")
    private String message;
}
