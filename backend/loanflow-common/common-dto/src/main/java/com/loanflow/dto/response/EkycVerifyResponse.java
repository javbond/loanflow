package com.loanflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "e-KYC OTP verification response")
public class EkycVerifyResponse {

    @Schema(description = "Whether verification was successful")
    private boolean verified;

    @Schema(description = "Transaction ID for this attempt", example = "TXN-abc123")
    private String transactionId;

    @Schema(description = "Verification status", example = "VERIFIED")
    private String status;

    @Schema(description = "Descriptive message", example = "e-KYC verification successful")
    private String message;

    @Schema(description = "Demographic data extracted from UIDAI e-KYC")
    private EkycData ekycData;

    @Schema(description = "Central KYC Registry number (assigned after CKYC submission)", example = "CKYC-2026-00012345")
    private String ckycNumber;
}
