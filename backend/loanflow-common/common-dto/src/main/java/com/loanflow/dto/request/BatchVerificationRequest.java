package com.loanflow.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch document verification.
 * Allows verifying or rejecting multiple documents in one operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchVerificationRequest {

    @NotEmpty(message = "At least one document ID is required")
    private List<String> documentIds;

    @NotNull(message = "Verifier ID is required")
    private UUID verifierId;

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String remarks;
}
