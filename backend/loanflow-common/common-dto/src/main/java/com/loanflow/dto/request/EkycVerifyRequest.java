package com.loanflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "e-KYC OTP verification request")
public class EkycVerifyRequest {

    @NotBlank(message = "Transaction ID is required")
    @Schema(description = "Transaction ID from OTP initiation", example = "TXN-abc123")
    private String transactionId;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    @Schema(description = "6-digit OTP received on Aadhaar-linked mobile", example = "123456")
    private String otp;
}
