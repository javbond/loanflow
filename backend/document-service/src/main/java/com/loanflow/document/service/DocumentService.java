package com.loanflow.document.service;

import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.dto.request.BatchVerificationRequest;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.request.DocumentVerificationRequest;
import com.loanflow.dto.response.DocumentCompletenessResponse;
import com.loanflow.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for document operations
 */
public interface DocumentService {

    // Upload operations
    DocumentResponse upload(MultipartFile file, DocumentUploadRequest request);

    // Get operations
    DocumentResponse getById(String id);

    DocumentResponse getByDocumentNumber(String documentNumber);

    String getDownloadUrl(String id);

    // List operations
    Page<DocumentResponse> getAll(Pageable pageable);

    Page<DocumentResponse> getByApplicationId(UUID applicationId, Pageable pageable);

    Page<DocumentResponse> getByCustomerId(UUID customerId, Pageable pageable);

    Page<DocumentResponse> getByApplicationIdAndStatus(UUID applicationId, DocumentStatus status, Pageable pageable);

    Page<DocumentResponse> getByApplicationIdAndCategory(UUID applicationId, DocumentCategory category, Pageable pageable);

    // Customer Portal operations
    Page<DocumentResponse> getByCustomerEmail(String email, Pageable pageable);

    // Verification operations
    DocumentResponse verify(String documentId, DocumentVerificationRequest request);

    List<DocumentResponse> batchVerify(BatchVerificationRequest request);

    // Completeness & checklist (US-021)
    DocumentCompletenessResponse getCompleteness(UUID applicationId, String loanType);

    Map<String, Long> getVerificationSummary(UUID applicationId);

    // OCR extraction data (US-022)
    Map<String, String> getExtractedData(String documentId);

    DocumentResponse updateExtractedData(String documentId, Map<String, String> data);

    // Delete operations
    void delete(String id);

    void hardDelete(String id);

    // Statistics
    long countByApplicationId(UUID applicationId);

    boolean hasAllRequiredDocuments(UUID applicationId, List<DocumentType> requiredTypes);

    long getPendingVerificationCount(UUID applicationId);
}
