package com.loanflow.document.service;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.mapper.DocumentMapper;
import com.loanflow.document.repository.DocumentRepository;
import com.loanflow.document.service.impl.DocumentServiceImpl;
import com.loanflow.dto.request.DocumentUploadRequest;
import com.loanflow.dto.request.DocumentVerificationRequest;
import com.loanflow.dto.response.DocumentResponse;
import com.loanflow.util.exception.ResourceNotFoundException;
import com.loanflow.util.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test Cases for DocumentService
 * Tests written FIRST, then implementation follows
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Tests")
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private DocumentMapper mapper;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private DocumentServiceImpl service;

    private String documentId;
    private UUID applicationId;
    private UUID customerId;
    private Document document;
    private DocumentResponse expectedResponse;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        documentId = "doc-" + UUID.randomUUID().toString().substring(0, 8);
        applicationId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        document = Document.builder()
                .id(documentId)
                .documentNumber("DOC-2024-000001")
                .applicationId(applicationId)
                .customerId(customerId)
                .documentType(DocumentType.PAN_CARD)
                .category(DocumentCategory.KYC)
                .originalFileName("pan_card.pdf")
                .contentType("application/pdf")
                .fileSize(1024000L)
                .status(DocumentStatus.PENDING)
                .build();

        expectedResponse = DocumentResponse.builder()
                .id(documentId)
                .documentNumber("DOC-2024-000001")
                .applicationId(applicationId)
                .customerId(customerId)
                .documentType("PAN_CARD")
                .category("KYC")
                .originalFileName("pan_card.pdf")
                .status("PENDING")
                .build();

        mockFile = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );
    }

    @Nested
    @DisplayName("Upload Document Tests")
    class UploadTests {

        @Test
        @DisplayName("Should upload document successfully")
        void shouldUploadDocument() {
            // Given
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType("PAN_CARD")
                    .build();

            when(storageService.upload(any(MultipartFile.class), anyString()))
                    .thenReturn("loanflow-documents/applications/123/kyc/pan.pdf");
            when(repository.save(any(Document.class))).thenReturn(document);
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            DocumentResponse result = service.upload(mockFile, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDocumentNumber()).startsWith("DOC-");

            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getApplicationId()).isEqualTo(applicationId);
        }

        @Test
        @DisplayName("Should reject invalid file type")
        void shouldRejectInvalidFileType() {
            // Given
            MultipartFile invalidFile = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/x-msdownload",
                    "EXE content".getBytes()
            );

            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .documentType("PAN_CARD")
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.upload(invalidFile, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("file type");
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void shouldRejectOversizedFile() {
            // Given
            byte[] largeContent = new byte[15 * 1024 * 1024]; // 15MB
            MultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large.pdf",
                    "application/pdf",
                    largeContent
            );

            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .documentType("PAN_CARD")
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.upload(largeFile, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");
        }

        @Test
        @DisplayName("Should handle storage failure gracefully")
        void shouldHandleStorageFailure() {
            // Given
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .documentType("PAN_CARD")
                    .build();

            when(storageService.upload(any(MultipartFile.class), anyString()))
                    .thenThrow(new StorageException("MinIO connection failed"));

            // When/Then
            assertThatThrownBy(() -> service.upload(mockFile, request))
                    .isInstanceOf(StorageException.class);
        }

        @Test
        @DisplayName("Should auto-categorize document based on type")
        void shouldAutoCategorizeDocument() {
            // Given
            DocumentUploadRequest request = DocumentUploadRequest.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType("SALARY_SLIP")
                    .build();

            Document incomeDoc = Document.builder()
                    .documentType(DocumentType.SALARY_SLIP)
                    .category(DocumentCategory.INCOME)
                    .build();

            when(storageService.upload(any(), anyString())).thenReturn("path");
            when(repository.save(any(Document.class))).thenReturn(incomeDoc);
            when(mapper.toResponse(any())).thenReturn(
                    DocumentResponse.builder().category("INCOME").build());

            // When
            DocumentResponse result = service.upload(mockFile, request);

            // Then
            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(DocumentCategory.INCOME);
        }
    }

    @Nested
    @DisplayName("Get Document Tests")
    class GetTests {

        @Test
        @DisplayName("Should get document by ID")
        void shouldGetById() {
            // Given
            when(repository.findById(documentId)).thenReturn(Optional.of(document));
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            DocumentResponse result = service.getById(documentId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(documentId);
        }

        @Test
        @DisplayName("Should throw exception when document not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(repository.findById(documentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(documentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Document");
        }

        @Test
        @DisplayName("Should get document by document number")
        void shouldGetByDocumentNumber() {
            // Given
            String docNumber = "DOC-2024-000001";
            when(repository.findByDocumentNumber(docNumber)).thenReturn(Optional.of(document));
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            DocumentResponse result = service.getByDocumentNumber(docNumber);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDocumentNumber()).isEqualTo(docNumber);
        }

        @Test
        @DisplayName("Should generate download URL")
        void shouldGenerateDownloadUrl() {
            // Given
            document.setStorageBucket("loanflow-documents");
            document.setStorageKey("applications/123/kyc/pan.pdf");

            when(repository.findById(documentId)).thenReturn(Optional.of(document));
            when(storageService.generatePresignedUrl(anyString(), anyString(), anyInt()))
                    .thenReturn("https://minio.local/download/pan.pdf?token=xyz");

            // When
            String downloadUrl = service.getDownloadUrl(documentId);

            // Then
            assertThat(downloadUrl).contains("minio");
            assertThat(downloadUrl).contains("token");
        }
    }

    @Nested
    @DisplayName("List Documents Tests")
    class ListTests {

        @Test
        @DisplayName("Should list documents by application ID")
        void shouldListByApplicationId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Document> page = new PageImpl<>(List.of(document), pageable, 1);

            when(repository.findByApplicationId(applicationId, pageable)).thenReturn(page);
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            Page<DocumentResponse> result = service.getByApplicationId(applicationId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should list documents by customer ID")
        void shouldListByCustomerId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Document> page = new PageImpl<>(List.of(document), pageable, 1);

            when(repository.findByCustomerId(customerId, pageable)).thenReturn(page);
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            Page<DocumentResponse> result = service.getByCustomerId(customerId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter documents by status")
        void shouldFilterByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Document> page = new PageImpl<>(List.of(document), pageable, 1);

            when(repository.findByApplicationIdAndStatus(applicationId, DocumentStatus.PENDING, pageable))
                    .thenReturn(page);
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            Page<DocumentResponse> result = service.getByApplicationIdAndStatus(
                    applicationId, DocumentStatus.PENDING, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter documents by category")
        void shouldFilterByCategory() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Document> page = new PageImpl<>(List.of(document), pageable, 1);

            when(repository.findByApplicationIdAndCategory(applicationId, DocumentCategory.KYC, pageable))
                    .thenReturn(page);
            when(mapper.toResponse(document)).thenReturn(expectedResponse);

            // When
            Page<DocumentResponse> result = service.getByApplicationIdAndCategory(
                    applicationId, DocumentCategory.KYC, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Verify Document Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should verify document successfully")
        void shouldVerifyDocument() {
            // Given
            UUID verifierId = UUID.randomUUID();
            DocumentVerificationRequest request = DocumentVerificationRequest.builder()
                    .verifierId(verifierId)
                    .approved(true)
                    .remarks("Document is valid")
                    .build();

            document.setStatus(DocumentStatus.UPLOADED);
            when(repository.findById(documentId)).thenReturn(Optional.of(document));
            when(repository.save(any(Document.class))).thenReturn(document);

            DocumentResponse verifiedResponse = DocumentResponse.builder()
                    .id(documentId)
                    .status("VERIFIED")
                    .build();
            when(mapper.toResponse(any(Document.class))).thenReturn(verifiedResponse);

            // When
            DocumentResponse result = service.verify(documentId, request);

            // Then
            assertThat(result.getStatus()).isEqualTo("VERIFIED");

            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.VERIFIED);
        }

        @Test
        @DisplayName("Should reject document with reason")
        void shouldRejectDocument() {
            // Given
            UUID verifierId = UUID.randomUUID();
            DocumentVerificationRequest request = DocumentVerificationRequest.builder()
                    .verifierId(verifierId)
                    .approved(false)
                    .remarks("Document is blurry")
                    .build();

            document.setStatus(DocumentStatus.UPLOADED);
            when(repository.findById(documentId)).thenReturn(Optional.of(document));
            when(repository.save(any(Document.class))).thenReturn(document);

            DocumentResponse rejectedResponse = DocumentResponse.builder()
                    .id(documentId)
                    .status("REJECTED")
                    .build();
            when(mapper.toResponse(any(Document.class))).thenReturn(rejectedResponse);

            // When
            DocumentResponse result = service.verify(documentId, request);

            // Then
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Should not verify already verified document")
        void shouldNotReverify() {
            // Given
            document.setStatus(DocumentStatus.VERIFIED);
            when(repository.findById(documentId)).thenReturn(Optional.of(document));

            DocumentVerificationRequest request = DocumentVerificationRequest.builder()
                    .verifierId(UUID.randomUUID())
                    .approved(true)
                    .build();

            // When/Then
            assertThatThrownBy(() -> service.verify(documentId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already verified");
        }
    }

    @Nested
    @DisplayName("Delete Document Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should soft delete document")
        void shouldSoftDelete() {
            // Given
            when(repository.findById(documentId)).thenReturn(Optional.of(document));

            // When
            service.delete(documentId);

            // Then
            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.DELETED);
        }

        @Test
        @DisplayName("Should delete from storage when hard delete")
        void shouldHardDelete() {
            // Given
            document.setStorageBucket("loanflow-documents");
            document.setStorageKey("applications/123/kyc/pan.pdf");
            when(repository.findById(documentId)).thenReturn(Optional.of(document));

            // When
            service.hardDelete(documentId);

            // Then
            verify(storageService).delete("loanflow-documents", "applications/123/kyc/pan.pdf");
            verify(repository).delete(document);
        }
    }

    @Nested
    @DisplayName("Document Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get document count by application")
        void shouldGetCountByApplication() {
            // Given
            when(repository.countByApplicationId(applicationId)).thenReturn(5L);

            // When
            long count = service.countByApplicationId(applicationId);

            // Then
            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should check if all required documents uploaded")
        void shouldCheckRequiredDocuments() {
            // Given
            List<DocumentType> requiredTypes = List.of(
                    DocumentType.PAN_CARD,
                    DocumentType.AADHAAR_CARD,
                    DocumentType.SALARY_SLIP
            );

            when(repository.findDistinctDocumentTypesByApplicationId(applicationId))
                    .thenReturn(requiredTypes);

            // When
            boolean allUploaded = service.hasAllRequiredDocuments(applicationId, requiredTypes);

            // Then
            assertThat(allUploaded).isTrue();
        }

        @Test
        @DisplayName("Should get pending verification count")
        void shouldGetPendingCount() {
            // Given
            when(repository.countByApplicationIdAndStatus(applicationId, DocumentStatus.UPLOADED))
                    .thenReturn(3L);

            // When
            long pendingCount = service.getPendingVerificationCount(applicationId);

            // Then
            assertThat(pendingCount).isEqualTo(3L);
        }
    }
}
