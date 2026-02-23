package com.loanflow.document.service.impl;

import com.loanflow.document.config.DocumentRequirementsConfig;
import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.DocumentService;
import com.loanflow.document.service.OcrExtractionResult;
import com.loanflow.document.service.OcrService;
import com.loanflow.document.service.StorageService;
import com.loanflow.document.service.VirusScanResult;
import com.loanflow.document.service.VirusScanService;
import com.loanflow.dto.request.BatchVerificationRequest;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.request.DocumentVerificationRequest;
import com.loanflow.dto.response.DocumentCompletenessResponse;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.dto.response.VerificationChecklistItem;
import com.loanflow.util.exception.BusinessException;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DocumentService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/tiff",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final String BUCKET_NAME = "loanflow-documents";

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final StorageService storageService;
    private final VirusScanService virusScanService;
    private final OcrService ocrService;
    private final DocumentRequirementsConfig requirementsConfig;

    @Override
    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentUploadRequest request) {
        log.info("Uploading document for application: {}", request.getApplicationId());

        // Validate file type
        validateFileType(file);

        // Validate file size
        validateFileSize(file);

        // Scan for viruses (US-020) — fail-closed for security
        VirusScanResult scanResult = virusScanService.scan(file);
        if (scanResult.isInfected()) {
            log.warn("Virus detected in upload for application {}: {}",
                    request.getApplicationId(), scanResult.getVirusName());
            throw new BusinessException("VIRUS_DETECTED",
                    "File rejected: virus detected (" + scanResult.getVirusName() +
                    "). Please scan your device and upload a clean file.");
        }
        if (!scanResult.isClean()) {
            log.error("Virus scan failed for application {}: {}",
                    request.getApplicationId(), scanResult.getErrorMessage());
            throw new BusinessException("VIRUS_SCAN_FAILED",
                    "Unable to verify file safety. Please try again later.");
        }

        // Parse document type
        DocumentType documentType = DocumentType.valueOf(request.getDocumentType());

        // Create document entity
        Document document = Document.builder()
                .applicationId(request.getApplicationId())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .documentType(documentType)
                .category(documentType.getCategory())
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .description(request.getDescription())
                .build();

        document.prePersist();

        // Generate storage path
        String storagePath = document.generateStoragePath();

        // Upload to storage
        String fullPath = storageService.upload(file, storagePath);

        // Set storage location
        String[] parts = fullPath.split("/", 2);
        document.setStorageLocation(BUCKET_NAME, storagePath);

        // Set expiry if applicable
        document.setDefaultExpiry();

        // Save document
        Document saved = repository.save(document);

        // Trigger OCR extraction (US-022) — non-blocking, best-effort
        try {
            byte[] content = file.getBytes();
            OcrExtractionResult ocrResult = ocrService.extract(
                    saved.getId(), content, file.getContentType(), request.getDocumentType());
            saved.setExtractedData(ocrResult.getExtractedFields());
            saved.setExtractionStatus(ocrResult.getStatus().name());
            saved.setExtractedText(truncateText(ocrResult.getExtractedText(), 5000));
            saved = repository.save(saved);
            log.info("OCR extraction completed for {}: status={}, fields={}",
                    saved.getId(), ocrResult.getStatus(), ocrResult.getExtractedFields().size());
        } catch (Exception e) {
            log.warn("OCR extraction failed for document {}: {}", saved.getId(), e.getMessage());
            saved.setExtractionStatus("FAILED");
            saved = repository.save(saved);
        }

        log.info("Document uploaded successfully: {}", saved.getDocumentNumber());
        return mapper.toResponse(saved);
    }

    @Override
    public DocumentResponse getById(String id) {
        Document document = findDocumentById(id);
        return mapper.toResponse(document);
    }

    @Override
    public DocumentResponse getByDocumentNumber(String documentNumber) {
        Document document = repository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "documentNumber", documentNumber));
        return mapper.toResponse(document);
    }

    @Override
    public String getDownloadUrl(String id) {
        Document document = findDocumentById(id);

        if (document.getStorageBucket() == null || document.getStorageKey() == null) {
            throw new IllegalStateException("Document not uploaded to storage");
        }

        return storageService.generatePresignedUrl(
                document.getStorageBucket(),
                document.getStorageKey(),
                15 // 15 minutes expiry
        );
    }

    @Override
    public Page<DocumentResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<DocumentResponse> getByApplicationId(UUID applicationId, Pageable pageable) {
        return repository.findByApplicationId(applicationId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<DocumentResponse> getByCustomerId(UUID customerId, Pageable pageable) {
        return repository.findByCustomerId(customerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<DocumentResponse> getByApplicationIdAndStatus(UUID applicationId, DocumentStatus status, Pageable pageable) {
        return repository.findByApplicationIdAndStatus(applicationId, status, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<DocumentResponse> getByApplicationIdAndCategory(UUID applicationId, DocumentCategory category, Pageable pageable) {
        return repository.findByApplicationIdAndCategory(applicationId, category, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public Page<DocumentResponse> getByCustomerEmail(String email, Pageable pageable) {
        log.debug("Fetching documents for customer email: {}", email);
        return repository.findByCustomerEmail(email, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public DocumentResponse verify(String documentId, DocumentVerificationRequest request) {
        Document document = findDocumentById(documentId);

        if (document.getStatus() == DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Document is already verified");
        }

        if (request.getApproved()) {
            document.verify(request.getVerifierId(), request.getRemarks());
            log.info("Document {} verified by {}", documentId, request.getVerifierId());
        } else {
            document.reject(request.getVerifierId(), request.getRemarks());
            log.info("Document {} rejected by {}: {}", documentId, request.getVerifierId(), request.getRemarks());
        }

        Document saved = repository.save(document);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Document document = findDocumentById(id);
        document.setStatus(DocumentStatus.DELETED);
        repository.save(document);
        log.info("Document {} soft deleted", id);
    }

    @Override
    @Transactional
    public void hardDelete(String id) {
        Document document = findDocumentById(id);

        // Delete from storage
        if (document.getStorageBucket() != null && document.getStorageKey() != null) {
            storageService.delete(document.getStorageBucket(), document.getStorageKey());
        }

        // Delete from database
        repository.delete(document);
        log.info("Document {} hard deleted", id);
    }

    @Override
    public long countByApplicationId(UUID applicationId) {
        return repository.countByApplicationId(applicationId);
    }

    @Override
    public boolean hasAllRequiredDocuments(UUID applicationId, List<DocumentType> requiredTypes) {
        List<DocumentType> uploadedTypes = repository.findDistinctDocumentTypesByApplicationId(applicationId);
        Set<DocumentType> uploadedSet = Set.copyOf(uploadedTypes);
        return uploadedSet.containsAll(requiredTypes);
    }

    @Override
    public long getPendingVerificationCount(UUID applicationId) {
        return repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.UPLOADED);
    }

    // ==================== US-021: Batch Verification & Completeness ====================

    @Override
    @Transactional
    public List<DocumentResponse> batchVerify(BatchVerificationRequest request) {
        log.info("Batch verifying {} documents by {}", request.getDocumentIds().size(), request.getVerifierId());

        List<DocumentResponse> results = new ArrayList<>();

        for (String docId : request.getDocumentIds()) {
            Document document = findDocumentById(docId);

            if (document.getStatus() == DocumentStatus.VERIFIED) {
                log.warn("Skipping already verified document: {}", docId);
                results.add(mapper.toResponse(document));
                continue;
            }

            if (request.getApproved()) {
                document.verify(request.getVerifierId(), request.getRemarks());
            } else {
                document.reject(request.getVerifierId(), request.getRemarks());
            }

            Document saved = repository.save(document);
            results.add(mapper.toResponse(saved));
        }

        log.info("Batch verification complete: {} documents processed", results.size());
        return results;
    }

    @Override
    public DocumentCompletenessResponse getCompleteness(UUID applicationId, String loanType) {
        log.debug("Checking document completeness for application {} (type: {})", applicationId, loanType);

        List<String> requiredDocTypes = requirementsConfig.getRequiredDocuments(loanType);

        // Get all documents for this application (unpaginated — small result set)
        List<Document> allDocs = repository.findByApplicationId(applicationId, PageRequest.of(0, 100))
                .getContent();

        // Build a map: documentType -> best document (prefer VERIFIED > UPLOADED > others)
        Map<String, Document> bestDocByType = new HashMap<>();
        for (Document doc : allDocs) {
            String type = doc.getDocumentType().name();
            Document existing = bestDocByType.get(type);
            if (existing == null || getBestStatusPriority(doc.getStatus()) > getBestStatusPriority(existing.getStatus())) {
                bestDocByType.put(type, doc);
            }
        }

        // Build checklist
        List<VerificationChecklistItem> checklist = new ArrayList<>();
        int totalUploaded = 0;
        int totalVerified = 0;
        int totalRejected = 0;

        for (String docTypeStr : requiredDocTypes) {
            Document doc = bestDocByType.get(docTypeStr);
            DocumentType docType;
            try {
                docType = DocumentType.valueOf(docTypeStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            boolean uploaded = doc != null && (doc.getStatus() == DocumentStatus.UPLOADED || doc.getStatus() == DocumentStatus.VERIFIED);
            boolean verified = doc != null && doc.getStatus() == DocumentStatus.VERIFIED;
            boolean rejected = doc != null && doc.getStatus() == DocumentStatus.REJECTED;

            if (uploaded || verified) totalUploaded++;
            if (verified) totalVerified++;
            if (rejected) totalRejected++;

            checklist.add(VerificationChecklistItem.builder()
                    .documentType(docTypeStr)
                    .label(formatDocumentTypeLabel(docTypeStr))
                    .category(docType.getCategory().name())
                    .mandatory(true)
                    .uploaded(uploaded || verified)
                    .verified(verified)
                    .rejected(rejected)
                    .documentId(doc != null ? doc.getId() : null)
                    .status(doc != null ? doc.getStatus().name() : "MISSING")
                    .build());
        }

        int totalRequired = requiredDocTypes.size();
        boolean complete = totalVerified >= totalRequired;
        int completionPct = totalRequired > 0 ? (totalVerified * 100 / totalRequired) : 0;

        return DocumentCompletenessResponse.builder()
                .applicationId(applicationId)
                .loanType(loanType)
                .totalRequired(totalRequired)
                .totalUploaded(totalUploaded)
                .totalVerified(totalVerified)
                .totalRejected(totalRejected)
                .complete(complete)
                .completionPercentage(completionPct)
                .checklist(checklist)
                .build();
    }

    @Override
    public Map<String, Long> getVerificationSummary(UUID applicationId) {
        log.debug("Getting verification summary for application {}", applicationId);

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("UPLOADED", repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.UPLOADED));
        summary.put("VERIFIED", repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.VERIFIED));
        summary.put("REJECTED", repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.REJECTED));
        summary.put("PENDING", repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.PENDING));
        summary.put("TOTAL", repository.countByApplicationId(applicationId));

        return summary;
    }

    // ==================== US-022: OCR Extracted Data ====================

    @Override
    public Map<String, String> getExtractedData(String documentId) {
        Document document = findDocumentById(documentId);
        return document.getExtractedData() != null ? document.getExtractedData() : Map.of();
    }

    @Override
    @Transactional
    public DocumentResponse updateExtractedData(String documentId, Map<String, String> data) {
        Document document = findDocumentById(documentId);
        document.setExtractedData(data);
        document.setExtractionStatus("REVIEWED");
        Document saved = repository.save(document);
        log.info("Updated extracted data for document {}: {} fields", documentId, data.size());
        return mapper.toResponse(saved);
    }

    // ==================== Private Methods ====================

    private int getBestStatusPriority(DocumentStatus status) {
        return switch (status) {
            case VERIFIED -> 3;
            case UPLOADED -> 2;
            case PENDING -> 1;
            default -> 0;
        };
    }

    private String formatDocumentTypeLabel(String type) {
        return Arrays.stream(type.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private Document findDocumentById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, JPEG, PNG, TIFF, DOC, DOCX");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
