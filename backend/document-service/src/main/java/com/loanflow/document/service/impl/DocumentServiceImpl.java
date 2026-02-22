package com.loanflow.document.service.impl;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.DocumentService;
import com.loanflow.document.service.StorageService;
import com.loanflow.document.service.VirusScanResult;
import com.loanflow.document.service.VirusScanService;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.request.DocumentVerificationRequest;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.util.exception.BusinessException;
import com.loanflow.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    // ==================== Private Methods ====================

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
}
