package com.loanflow.document.integration;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import com.loanflow.document.repository.DocumentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DocumentRepository with embedded MongoDB.
 * Tests CRUD operations, custom queries, and document lifecycle
 * against an actual MongoDB instance via Flapdoodle.
 */
@DataMongoTest
@ActiveProfiles("integration-test")
class DocumentRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    private UUID testApplicationId;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        testApplicationId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();
    }

    // ==================== Test Data Builders ====================

    private Document buildDocument(DocumentType type, DocumentStatus status) {
        Document doc = new Document();
        doc.setApplicationId(testApplicationId);
        doc.setCustomerId(testCustomerId);
        doc.setCustomerEmail("test@example.com");
        doc.setDocumentType(type);
        doc.setCategory(type.getCategory());
        doc.setOriginalFileName("test-" + type.name().toLowerCase() + ".pdf");
        doc.setContentType("application/pdf");
        doc.setFileSize(1024L);
        doc.setStatus(status);
        doc.setStorageBucket("test-bucket");
        doc.setStorageKey("test/" + UUID.randomUUID() + ".pdf");
        doc.setChecksum("abc123hash");
        doc.setUploadedAt(LocalDateTime.now());
        doc.setUploadedBy(UUID.randomUUID());
        doc.setVersion(1);
        doc.setDocumentNumber(Document.generateDocumentNumber());
        return doc;
    }

    // ==================== CRUD Tests ====================

    @Nested
    @DisplayName("Basic CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and retrieve document by ID")
        void shouldSaveAndRetrieve() {
            Document doc = buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED);
            Document saved = documentRepository.save(doc);

            Optional<Document> found = documentRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getDocumentType()).isEqualTo(DocumentType.AADHAAR_CARD);
            assertThat(found.get().getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        }

        @Test
        @DisplayName("Should find document by document number")
        void shouldFindByDocumentNumber() {
            Document doc = buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.UPLOADED);
            Document saved = documentRepository.save(doc);

            Optional<Document> found = documentRepository.findByDocumentNumber(saved.getDocumentNumber());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Should count documents by application ID")
        void shouldCountByApplicationId() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.VOTER_ID, DocumentStatus.PENDING));

            long count = documentRepository.countByApplicationId(testApplicationId);

            assertThat(count).isEqualTo(3);
        }
    }

    // ==================== Query Tests ====================

    @Nested
    @DisplayName("Custom Queries")
    class QueryTests {

        @Test
        @DisplayName("Should find documents by application ID with pagination")
        void shouldFindByApplicationId() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.VERIFIED));

            Page<Document> page = documentRepository.findByApplicationId(
                    testApplicationId, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should find documents by status")
        void shouldFindByApplicationIdAndStatus() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.VERIFIED));
            documentRepository.save(buildDocument(DocumentType.VOTER_ID, DocumentStatus.UPLOADED));

            Page<Document> uploaded = documentRepository.findByApplicationIdAndStatus(
                    testApplicationId, DocumentStatus.UPLOADED, PageRequest.of(0, 10));

            assertThat(uploaded.getContent()).hasSize(2);
            assertThat(uploaded.getContent()).allMatch(d -> d.getStatus() == DocumentStatus.UPLOADED);
        }

        @Test
        @DisplayName("Should find documents by customer email")
        void shouldFindByCustomerEmail() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));

            Page<Document> page = documentRepository.findByCustomerEmail(
                    "test@example.com", PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should count pending verification documents")
        void shouldCountPendingVerification() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.VOTER_ID, DocumentStatus.VERIFIED));

            long count = documentRepository.countByApplicationIdAndStatus(
                    testApplicationId, DocumentStatus.UPLOADED);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should check existence by application ID and document type")
        void shouldCheckExistence() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));

            boolean exists = documentRepository.existsByApplicationIdAndDocumentType(
                    testApplicationId, DocumentType.AADHAAR_CARD);
            boolean notExists = documentRepository.existsByApplicationIdAndDocumentType(
                    testApplicationId, DocumentType.BANK_STATEMENT);

            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    // ==================== Verification Tests ====================

    @Nested
    @DisplayName("Document Verification Flow")
    class VerificationTests {

        @Test
        @DisplayName("Should update document status to VERIFIED with remarks")
        void shouldVerifyDocument() {
            Document doc = buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED);
            Document saved = documentRepository.save(doc);

            saved.setStatus(DocumentStatus.VERIFIED);
            saved.setVerifiedAt(LocalDateTime.now());
            saved.setVerifiedBy(UUID.randomUUID());
            saved.setVerificationRemarks("Document verified — all details match");
            documentRepository.save(saved);

            Optional<Document> verified = documentRepository.findById(saved.getId());

            assertThat(verified).isPresent();
            assertThat(verified.get().getStatus()).isEqualTo(DocumentStatus.VERIFIED);
            assertThat(verified.get().getVerificationRemarks()).contains("verified");
        }

        @Test
        @DisplayName("Should update document status to REJECTED")
        void shouldRejectDocument() {
            Document doc = buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.UPLOADED);
            Document saved = documentRepository.save(doc);

            saved.setStatus(DocumentStatus.REJECTED);
            saved.setVerifiedAt(LocalDateTime.now());
            saved.setVerifiedBy(UUID.randomUUID());
            saved.setVerificationRemarks("Blurry image — cannot read income details");
            documentRepository.save(saved);

            Optional<Document> rejected = documentRepository.findById(saved.getId());

            assertThat(rejected).isPresent();
            assertThat(rejected.get().getStatus()).isEqualTo(DocumentStatus.REJECTED);
        }
    }

    // ==================== OCR Data Tests ====================

    @Nested
    @DisplayName("OCR Extracted Data")
    class OcrTests {

        @Test
        @DisplayName("Should store and retrieve OCR extracted data")
        void shouldStoreExtractedData() {
            Document doc = buildDocument(DocumentType.PAN_CARD, DocumentStatus.UPLOADED);
            doc.setExtractedData(java.util.Map.of(
                    "name", "Rahul Sharma",
                    "pan_number", "ABCDE1234F",
                    "dob", "15-05-1990"
            ));
            doc.setExtractionStatus("SUCCESS");
            Document saved = documentRepository.save(doc);

            Optional<Document> found = documentRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getExtractedData()).containsEntry("name", "Rahul Sharma");
            assertThat(found.get().getExtractionStatus()).isEqualTo("SUCCESS");
        }
    }

    // ==================== Deletion Tests ====================

    @Nested
    @DisplayName("Document Deletion")
    class DeletionTests {

        @Test
        @DisplayName("Should delete all documents by application ID")
        void shouldDeleteByApplicationId() {
            documentRepository.save(buildDocument(DocumentType.AADHAAR_CARD, DocumentStatus.UPLOADED));
            documentRepository.save(buildDocument(DocumentType.SALARY_SLIP, DocumentStatus.UPLOADED));

            documentRepository.deleteByApplicationId(testApplicationId);

            long count = documentRepository.countByApplicationId(testApplicationId);
            assertThat(count).isEqualTo(0);
        }
    }
}
