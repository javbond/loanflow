package com.loanflow.document.service;

/**
 * Service interface for OCR text extraction from documents (US-022).
 * Implementations extract text and structured data (PAN, Aadhaar, etc.) from uploaded files.
 */
public interface OcrService {

    /**
     * Extract text and structured fields from document content.
     *
     * @param documentId   the document ID for logging
     * @param content      raw file bytes
     * @param contentType  MIME type (e.g., "application/pdf", "image/jpeg")
     * @param documentType document type for field-specific extraction (e.g., "PAN_CARD")
     * @return extraction result with text and structured fields
     */
    OcrExtractionResult extract(String documentId, byte[] content, String contentType, String documentType);
}
