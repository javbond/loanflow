package com.loanflow.document.service;

import com.loanflow.document.config.DocumentRequirementsConfig;
import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.impl.DocumentServiceImpl;
import com.loanflow.dto.request.BatchVerificationRequest;
import com.loanflow.dto.response.DocumentCompletenessResponse;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.dto.response.VerificationChecklistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * US-021: Document Verification Workflow Tests
 * Tests for verification checklist, completeness, batch verify, and verification summary.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Document Completeness & Batch Verification Tests (US-021)")
class DocumentCompletenessTest {

    @Mock private DocumentRepository repository;
    @Mock private DocumentMapper mapper;
    @Mock private StorageService storageService;
    @Mock private VirusScanService virusScanService;
    @Mock private OcrService ocrService;
    @Mock private DocumentRequirementsConfig requirementsConfig;

    @InjectMocks
    private DocumentServiceImpl service;

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
    }

    // ==================== Completeness Tests ====================

    @Nested
    @DisplayName("Document Completeness")
    class CompletenessTests {

        @Test
        @DisplayName("Should return full checklist for HOME_LOAN with all required docs")
        void shouldReturnFullChecklistForHomeLoan() {
            // Given
            List<String> requiredDocs = List.of("PAN_CARD", "AADHAAR_CARD", "SALARY_SLIP", "BANK_STATEMENT", "PROPERTY_DEED", "SALE_AGREEMENT");
            when(requirementsConfig.getRequiredDocuments("HOME_LOAN")).thenReturn(requiredDocs);
            when(repository.findByApplicationId(eq(applicationId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // When
            DocumentCompletenessResponse response = service.getCompleteness(applicationId, "HOME_LOAN");

            // Then
            assertThat(response.getTotalRequired()).isEqualTo(6);
            assertThat(response.getChecklist()).hasSize(6);
            assertThat(response.isComplete()).isFalse();
            assertThat(response.getCompletionPercentage()).isEqualTo(0);
            assertThat(response.getApplicationId()).isEqualTo(applicationId);
            assertThat(response.getLoanType()).isEqualTo("HOME_LOAN");
        }

        @Test
        @DisplayName("Should mark uploaded documents in checklist")
        void shouldMarkUploadedDocumentsInChecklist() {
            // Given
            List<String> requiredDocs = List.of("PAN_CARD", "AADHAAR_CARD", "SALARY_SLIP");
            when(requirementsConfig.getRequiredDocuments("PERSONAL_LOAN")).thenReturn(requiredDocs);

            Document panDoc = Document.builder()
                    .id("doc-1")
                    .applicationId(applicationId)
                    .documentType(DocumentType.PAN_CARD)
                    .status(DocumentStatus.UPLOADED)
                    .build();

            when(repository.findByApplicationId(eq(applicationId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(panDoc)));

            // When
            DocumentCompletenessResponse response = service.getCompleteness(applicationId, "PERSONAL_LOAN");

            // Then
            assertThat(response.getTotalUploaded()).isEqualTo(1);
            assertThat(response.getTotalRequired()).isEqualTo(3);

            VerificationChecklistItem panItem = response.getChecklist().stream()
                    .filter(i -> i.getDocumentType().equals("PAN_CARD"))
                    .findFirst().orElseThrow();
            assertThat(panItem.isUploaded()).isTrue();
            assertThat(panItem.isVerified()).isFalse();
            assertThat(panItem.getDocumentId()).isEqualTo("doc-1");

            VerificationChecklistItem aadhaarItem = response.getChecklist().stream()
                    .filter(i -> i.getDocumentType().equals("AADHAAR_CARD"))
                    .findFirst().orElseThrow();
            assertThat(aadhaarItem.isUploaded()).isFalse();
            assertThat(aadhaarItem.getStatus()).isEqualTo("MISSING");
        }

        @Test
        @DisplayName("Should mark verified documents in checklist and calculate completion")
        void shouldMarkVerifiedDocumentsInChecklist() {
            // Given
            List<String> requiredDocs = List.of("PAN_CARD", "AADHAAR_CARD");
            when(requirementsConfig.getRequiredDocuments("PERSONAL_LOAN")).thenReturn(requiredDocs);

            Document panVerified = Document.builder()
                    .id("doc-1")
                    .applicationId(applicationId)
                    .documentType(DocumentType.PAN_CARD)
                    .status(DocumentStatus.VERIFIED)
                    .build();
            Document aadhaarVerified = Document.builder()
                    .id("doc-2")
                    .applicationId(applicationId)
                    .documentType(DocumentType.AADHAAR_CARD)
                    .status(DocumentStatus.VERIFIED)
                    .build();

            when(repository.findByApplicationId(eq(applicationId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(panVerified, aadhaarVerified)));

            // When
            DocumentCompletenessResponse response = service.getCompleteness(applicationId, "PERSONAL_LOAN");

            // Then
            assertThat(response.isComplete()).isTrue();
            assertThat(response.getCompletionPercentage()).isEqualTo(100);
            assertThat(response.getTotalVerified()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate correct completion percentage")
        void shouldCalculateCompletionPercentage() {
            // Given
            List<String> requiredDocs = List.of("PAN_CARD", "AADHAAR_CARD", "SALARY_SLIP", "BANK_STATEMENT");
            when(requirementsConfig.getRequiredDocuments("PERSONAL_LOAN")).thenReturn(requiredDocs);

            Document panVerified = Document.builder()
                    .id("doc-1")
                    .applicationId(applicationId)
                    .documentType(DocumentType.PAN_CARD)
                    .status(DocumentStatus.VERIFIED)
                    .build();

            when(repository.findByApplicationId(eq(applicationId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(panVerified)));

            // When
            DocumentCompletenessResponse response = service.getCompleteness(applicationId, "PERSONAL_LOAN");

            // Then
            assertThat(response.getCompletionPercentage()).isEqualTo(25); // 1 out of 4
            assertThat(response.isComplete()).isFalse();
        }

        @Test
        @DisplayName("Should return empty checklist for unknown loan type")
        void shouldReturnEmptyChecklistForUnknownLoanType() {
            // Given
            when(requirementsConfig.getRequiredDocuments("UNKNOWN_TYPE")).thenReturn(List.of());
            when(repository.findByApplicationId(eq(applicationId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // When
            DocumentCompletenessResponse response = service.getCompleteness(applicationId, "UNKNOWN_TYPE");

            // Then
            assertThat(response.getTotalRequired()).isEqualTo(0);
            assertThat(response.getChecklist()).isEmpty();
            assertThat(response.getCompletionPercentage()).isEqualTo(0);
        }
    }

    // ==================== Batch Verification Tests ====================

    @Nested
    @DisplayName("Batch Verification")
    class BatchVerificationTests {

        @Test
        @DisplayName("Should batch verify multiple documents successfully")
        void shouldBatchVerifyMultipleDocuments() {
            // Given
            UUID verifierId = UUID.randomUUID();
            String docId1 = "doc-1";
            String docId2 = "doc-2";

            Document doc1 = Document.builder()
                    .id(docId1)
                    .applicationId(applicationId)
                    .documentType(DocumentType.PAN_CARD)
                    .status(DocumentStatus.UPLOADED)
                    .build();
            Document doc2 = Document.builder()
                    .id(docId2)
                    .applicationId(applicationId)
                    .documentType(DocumentType.AADHAAR_CARD)
                    .status(DocumentStatus.UPLOADED)
                    .build();

            when(repository.findById(docId1)).thenReturn(Optional.of(doc1));
            when(repository.findById(docId2)).thenReturn(Optional.of(doc2));
            when(repository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Document.class))).thenReturn(
                    DocumentResponse.builder().status("VERIFIED").build());

            BatchVerificationRequest request = new BatchVerificationRequest();
            request.setDocumentIds(List.of(docId1, docId2));
            request.setVerifierId(verifierId);
            request.setApproved(true);
            request.setRemarks("All documents verified");

            // When
            List<DocumentResponse> results = service.batchVerify(request);

            // Then
            assertThat(results).hasSize(2);
            verify(repository, times(2)).save(any(Document.class));
        }

        @Test
        @DisplayName("Should skip already verified documents in batch")
        void shouldSkipAlreadyVerifiedInBatch() {
            // Given
            UUID verifierId = UUID.randomUUID();
            String docId1 = "doc-1";

            Document alreadyVerified = Document.builder()
                    .id(docId1)
                    .applicationId(applicationId)
                    .documentType(DocumentType.PAN_CARD)
                    .status(DocumentStatus.VERIFIED)
                    .build();

            when(repository.findById(docId1)).thenReturn(Optional.of(alreadyVerified));
            when(mapper.toResponse(alreadyVerified)).thenReturn(
                    DocumentResponse.builder().id(docId1).status("VERIFIED").build());

            BatchVerificationRequest request = new BatchVerificationRequest();
            request.setDocumentIds(List.of(docId1));
            request.setVerifierId(verifierId);
            request.setApproved(true);

            // When
            List<DocumentResponse> results = service.batchVerify(request);

            // Then
            assertThat(results).hasSize(1);
            verify(repository, never()).save(any(Document.class)); // not saved again
        }
    }

    // ==================== Verification Summary Tests ====================

    @Nested
    @DisplayName("Verification Summary")
    class VerificationSummaryTests {

        @Test
        @DisplayName("Should return correct verification summary counts")
        void shouldReturnVerificationSummary() {
            // Given
            when(repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.UPLOADED)).thenReturn(3L);
            when(repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.VERIFIED)).thenReturn(2L);
            when(repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.REJECTED)).thenReturn(1L);
            when(repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.PENDING)).thenReturn(0L);
            when(repository.countByApplicationId(applicationId)).thenReturn(6L);

            // When
            Map<String, Long> summary = service.getVerificationSummary(applicationId);

            // Then
            assertThat(summary).containsEntry("UPLOADED", 3L);
            assertThat(summary).containsEntry("VERIFIED", 2L);
            assertThat(summary).containsEntry("REJECTED", 1L);
            assertThat(summary).containsEntry("PENDING", 0L);
            assertThat(summary).containsEntry("TOTAL", 6L);
        }
    }
}
