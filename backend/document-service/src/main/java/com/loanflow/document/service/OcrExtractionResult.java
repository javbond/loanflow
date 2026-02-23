package com.loanflow.document.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of OCR text extraction from a document (US-022).
 * Contains extracted raw text and structured fields (e.g., PAN number, Aadhaar).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrExtractionResult {

    public enum Status {
        SUCCESS, PARTIAL, FAILED
    }

    private String documentId;

    private String documentType;

    private String extractedText;

    @Builder.Default
    private Map<String, String> extractedFields = new HashMap<>();

    private double confidence;

    private Status status;

    private String errorMessage;

    // ==================== Factory Methods ====================

    public static OcrExtractionResult success(String documentId, String extractedText,
                                               Map<String, String> fields, double confidence) {
        return OcrExtractionResult.builder()
                .documentId(documentId)
                .extractedText(extractedText)
                .extractedFields(fields != null ? fields : new HashMap<>())
                .confidence(confidence)
                .status(Status.SUCCESS)
                .build();
    }

    public static OcrExtractionResult partial(String documentId, String extractedText,
                                               Map<String, String> fields, double confidence) {
        return OcrExtractionResult.builder()
                .documentId(documentId)
                .extractedText(extractedText)
                .extractedFields(fields != null ? fields : new HashMap<>())
                .confidence(confidence)
                .status(Status.PARTIAL)
                .build();
    }

    public static OcrExtractionResult failed(String documentId, String errorMessage) {
        return OcrExtractionResult.builder()
                .documentId(documentId)
                .extractedFields(new HashMap<>())
                .confidence(0)
                .status(Status.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    // ==================== Convenience Methods ====================

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isPartial() {
        return status == Status.PARTIAL;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    public boolean hasExtractedFields() {
        return extractedFields != null && !extractedFields.isEmpty();
    }
}
