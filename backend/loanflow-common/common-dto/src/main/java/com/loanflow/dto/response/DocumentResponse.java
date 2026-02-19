package com.loanflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for document operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private String id;

    private String documentNumber;

    private UUID applicationId;

    private UUID customerId;

    private String documentType;

    private String category;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private String formattedFileSize;

    private String status;

    private String storageBucket;

    private String storageKey;

    private String downloadUrl;

    private LocalDateTime uploadedAt;

    private UUID uploadedBy;

    private LocalDateTime verifiedAt;

    private UUID verifiedBy;

    private String verificationRemarks;

    private Boolean aadhaarVerified;

    private Boolean panVerified;

    private LocalDateTime expiryDate;

    private Boolean expired;

    private Boolean expiringSoon;

    private Integer version;

    private String previousVersionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
