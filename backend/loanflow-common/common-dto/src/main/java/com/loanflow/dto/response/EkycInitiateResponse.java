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
@Schema(description = "e-KYC OTP initiation response")
public class EkycInitiateResponse {

    @Schema(description = "Unique transaction ID for this verification attempt", example = "TXN-abc123")
    private String transactionId;

    @Schema(description = "Masked mobile number linked to Aadhaar", example = "XXXXXX7890")
    private String maskedMobile;

    @Schema(description = "Current verification status", example = "OTP_SENT")
    private String status;

    @Schema(description = "Descriptive message", example = "OTP sent to Aadhaar-linked mobile")
    private String message;
}
