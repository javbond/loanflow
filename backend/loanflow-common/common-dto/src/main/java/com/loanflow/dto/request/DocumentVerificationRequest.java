package com.loanflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for document verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVerificationRequest {

    @NotNull(message = "Verifier ID is required")
    private UUID verifierId;

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String remarks;
}
