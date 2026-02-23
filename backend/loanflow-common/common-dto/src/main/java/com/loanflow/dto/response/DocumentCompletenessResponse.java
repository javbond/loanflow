package com.loanflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response showing document completeness for a loan application.
 * Includes checklist of required documents and their upload/verification status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCompletenessResponse {

    private UUID applicationId;

    private String loanType;

    private int totalRequired;

    private int totalUploaded;

    private int totalVerified;

    private int totalRejected;

    private boolean complete;

    private int completionPercentage;

    private List<VerificationChecklistItem> checklist;
}
