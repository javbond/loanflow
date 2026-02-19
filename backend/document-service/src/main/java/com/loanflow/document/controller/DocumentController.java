package com.loanflow.document.controller;

import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.service.DocumentService;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.request.DocumentVerificationRequest;
import com.loanflow.dto.response.ApiResponse;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller for Document operations
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Management", description = "APIs for document upload, verification, and management")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid DocumentUploadRequest request) {

        log.info("Uploading document for application: {}", request.getApplicationId());
        DocumentResponse response = documentService.upload(file, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(@PathVariable String id) {
        DocumentResponse response = documentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{documentNumber}")
    @Operation(summary = "Get document by document number")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> getByDocumentNumber(
            @PathVariable String documentNumber) {
        DocumentResponse response = documentService.getByDocumentNumber(documentNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get download URL for document")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(@PathVariable String id) {
        String url = documentService.getDownloadUrl(id);
        return ResponseEntity.ok(ApiResponse.success(url));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "List documents by application")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<PageResponse<DocumentResponse>> getByApplicationId(
            @PathVariable UUID applicationId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DocumentResponse> page = documentService.getByApplicationId(applicationId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List documents by customer")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<PageResponse<DocumentResponse>> getByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DocumentResponse> page = documentService.getByCustomerId(customerId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/application/{applicationId}/status/{status}")
    @Operation(summary = "List documents by application and status")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<PageResponse<DocumentResponse>> getByApplicationIdAndStatus(
            @PathVariable UUID applicationId,
            @PathVariable DocumentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DocumentResponse> page = documentService.getByApplicationIdAndStatus(applicationId, status, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/application/{applicationId}/category/{category}")
    @Operation(summary = "List documents by application and category")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<PageResponse<DocumentResponse>> getByApplicationIdAndCategory(
            @PathVariable UUID applicationId,
            @PathVariable DocumentCategory category,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DocumentResponse> page = documentService.getByApplicationIdAndCategory(applicationId, category, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify or reject a document")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'SENIOR_UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> verify(
            @PathVariable String id,
            @RequestBody @Valid DocumentVerificationRequest request) {

        log.info("Verifying document: {} by {}", id, request.getVerifierId());
        DocumentResponse response = documentService.verify(id, request);

        String message = request.getApproved() ? "Document verified successfully" : "Document rejected";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a document")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        log.info("Soft deleting document: {}", id);
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Permanently delete a document")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable String id) {
        log.info("Hard deleting document: {}", id);
        documentService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Document permanently deleted", null));
    }

    @GetMapping("/application/{applicationId}/count")
    @Operation(summary = "Get document count for application")
    public ResponseEntity<ApiResponse<Long>> countByApplicationId(@PathVariable UUID applicationId) {
        long count = documentService.countByApplicationId(applicationId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/application/{applicationId}/pending-count")
    @Operation(summary = "Get pending verification count for application")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'UNDERWRITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getPendingVerificationCount(
            @PathVariable UUID applicationId) {
        long count = documentService.getPendingVerificationCount(applicationId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
