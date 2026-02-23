package com.loanflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single item in the document verification checklist.
 * Shows whether a required document has been uploaded and verified.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationChecklistItem {

    private String documentType;

    private String label;

    private String category;

    private boolean mandatory;

    private boolean uploaded;

    private boolean verified;

    private boolean rejected;

    private String documentId;

    private String status;
}
