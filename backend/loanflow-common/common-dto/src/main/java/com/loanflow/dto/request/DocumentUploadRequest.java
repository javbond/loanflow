package com.loanflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for document upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    private UUID customerId;

    @NotBlank(message = "Document type is required")
    private String documentType;

    private String description;

    private LocalDateTime expiryDate;

    private String password; // For password-protected PDFs
}
