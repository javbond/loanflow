package com.loanflow.document.domain;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Cases for Document Entity
 * Tests written FIRST, then implementation follows
 */
@DisplayName("Document Entity Tests")
class DocumentTest {

    private Document document;
    private UUID applicationId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        document = Document.builder()
                .applicationId(applicationId)
                .customerId(customerId)
                .documentType(DocumentType.PAN_CARD)
                .category(DocumentCategory.KYC)
                .originalFileName("pan_card.pdf")
                .contentType("application/pdf")
                .fileSize(1024000L)
                .build();
        document.prePersist(); // Simulate MongoDB lifecycle callback
    }

    @Nested
    @DisplayName("Document Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create document with PENDING status by default")
        void shouldCreateWithPendingStatus() {
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);
        }

        @Test
        @DisplayName("Should generate document number on creation")
        void shouldGenerateDocumentNumber() {
            assertThat(document.getDocumentNumber()).isNotNull();
            assertThat(document.getDocumentNumber()).startsWith("DOC-");
        }

        @Test
        @DisplayName("Should set upload timestamp on creation")
        void shouldSetUploadTimestamp() {
            assertThat(document.getUploadedAt()).isNotNull();
            assertThat(document.getUploadedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should store file metadata correctly")
        void shouldStoreFileMetadata() {
            assertThat(document.getOriginalFileName()).isEqualTo("pan_card.pdf");
            assertThat(document.getContentType()).isEqualTo("application/pdf");
            assertThat(document.getFileSize()).isEqualTo(1024000L);
        }

        @Test
        @DisplayName("Should associate with application and customer")
        void shouldAssociateWithApplicationAndCustomer() {
            assertThat(document.getApplicationId()).isEqualTo(applicationId);
            assertThat(document.getCustomerId()).isEqualTo(customerId);
        }
    }

    @Nested
    @DisplayName("Document Type and Category Tests")
    class TypeCategoryTests {

        @Test
        @DisplayName("Should categorize PAN as KYC document")
        void shouldCategorizePanAsKyc() {
            document.setDocumentType(DocumentType.PAN_CARD);
            assertThat(document.getCategory()).isEqualTo(DocumentCategory.KYC);
        }

        @Test
        @DisplayName("Should categorize Aadhaar as KYC document")
        void shouldCategorizeAadhaarAsKyc() {
            Document aadhaarDoc = Document.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType(DocumentType.AADHAAR_CARD)
                    .originalFileName("aadhaar.pdf")
                    .contentType("application/pdf")
                    .fileSize(500000L)
                    .build();

            assertThat(aadhaarDoc.getCategory()).isEqualTo(DocumentCategory.KYC);
        }

        @Test
        @DisplayName("Should categorize Salary Slip as INCOME document")
        void shouldCategorizeSalarySlipAsIncome() {
            Document salaryDoc = Document.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType(DocumentType.SALARY_SLIP)
                    .originalFileName("salary_june.pdf")
                    .contentType("application/pdf")
                    .fileSize(200000L)
                    .build();

            assertThat(salaryDoc.getCategory()).isEqualTo(DocumentCategory.INCOME);
        }

        @Test
        @DisplayName("Should categorize Bank Statement as FINANCIAL document")
        void shouldCategorizeBankStatementAsFinancial() {
            Document bankDoc = Document.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType(DocumentType.BANK_STATEMENT)
                    .originalFileName("statement.pdf")
                    .contentType("application/pdf")
                    .fileSize(300000L)
                    .build();

            assertThat(bankDoc.getCategory()).isEqualTo(DocumentCategory.FINANCIAL);
        }

        @Test
        @DisplayName("Should categorize Property Deed as PROPERTY document")
        void shouldCategorizePropertyDeedAsProperty() {
            Document propertyDoc = Document.builder()
                    .applicationId(applicationId)
                    .customerId(customerId)
                    .documentType(DocumentType.PROPERTY_DEED)
                    .originalFileName("deed.pdf")
                    .contentType("application/pdf")
                    .fileSize(1500000L)
                    .build();

            assertThat(propertyDoc.getCategory()).isEqualTo(DocumentCategory.PROPERTY);
        }
    }

    @Nested
    @DisplayName("Document Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should verify document successfully")
        void shouldVerifyDocument() {
            UUID verifierId = UUID.randomUUID();

            document.verify(verifierId, "Document verified successfully");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
            assertThat(document.getVerifiedBy()).isEqualTo(verifierId);
            assertThat(document.getVerifiedAt()).isNotNull();
            assertThat(document.getVerificationRemarks()).isEqualTo("Document verified successfully");
        }

        @Test
        @DisplayName("Should reject document with reason")
        void shouldRejectDocumentWithReason() {
            UUID verifierId = UUID.randomUUID();

            document.reject(verifierId, "Document is blurry and unreadable");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.REJECTED);
            assertThat(document.getVerifiedBy()).isEqualTo(verifierId);
            assertThat(document.getVerificationRemarks()).isEqualTo("Document is blurry and unreadable");
        }

        @Test
        @DisplayName("Should require reupload when rejected")
        void shouldRequireReuploadWhenRejected() {
            document.reject(UUID.randomUUID(), "Invalid document");

            assertThat(document.requiresReupload()).isTrue();
        }

        @Test
        @DisplayName("Should not allow verification of already verified document")
        void shouldNotAllowReverification() {
            document.verify(UUID.randomUUID(), "First verification");

            assertThatThrownBy(() -> document.verify(UUID.randomUUID(), "Second verification"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already verified");
        }

        @Test
        @DisplayName("Should allow re-verification of rejected document")
        void shouldAllowReverificationOfRejected() {
            UUID verifier1 = UUID.randomUUID();
            UUID verifier2 = UUID.randomUUID();

            document.reject(verifier1, "First check failed");
            document.resetForReupload();
            document.verify(verifier2, "Resubmitted document is valid");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
        }
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should validate allowed file types")
        void shouldValidateAllowedFileTypes() {
            assertThat(document.isAllowedFileType()).isTrue();

            document.setContentType("application/exe");
            assertThat(document.isAllowedFileType()).isFalse();
        }

        @Test
        @DisplayName("Should validate maximum file size (10MB)")
        void shouldValidateMaxFileSize() {
            document.setFileSize(5 * 1024 * 1024L); // 5MB
            assertThat(document.isWithinSizeLimit()).isTrue();

            document.setFileSize(15 * 1024 * 1024L); // 15MB
            assertThat(document.isWithinSizeLimit()).isFalse();
        }

        @Test
        @DisplayName("Should get file extension from filename")
        void shouldGetFileExtension() {
            assertThat(document.getFileExtension()).isEqualTo("pdf");

            document.setOriginalFileName("photo.jpeg");
            assertThat(document.getFileExtension()).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("Should format file size for display")
        void shouldFormatFileSizeForDisplay() {
            document.setFileSize(1024L);
            assertThat(document.getFormattedFileSize()).isEqualTo("1.0 KB");

            document.setFileSize(1024 * 1024L);
            assertThat(document.getFormattedFileSize()).isEqualTo("1.0 MB");
        }
    }

    @Nested
    @DisplayName("Storage Path Tests")
    class StoragePathTests {

        @Test
        @DisplayName("Should generate storage path based on application")
        void shouldGenerateStoragePath() {
            String storagePath = document.generateStoragePath();

            assertThat(storagePath).contains(applicationId.toString());
            assertThat(storagePath).contains("KYC");
            assertThat(storagePath).endsWith(".pdf");
        }

        @Test
        @DisplayName("Should set storage location after upload")
        void shouldSetStorageLocation() {
            String bucket = "loanflow-documents";
            String objectKey = "applications/" + applicationId + "/kyc/pan_card_123.pdf";

            document.setStorageLocation(bucket, objectKey);

            assertThat(document.getStorageBucket()).isEqualTo(bucket);
            assertThat(document.getStorageKey()).isEqualTo(objectKey);
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        }

        @Test
        @DisplayName("Should mark as upload failed")
        void shouldMarkAsUploadFailed() {
            document.markUploadFailed("Storage service unavailable");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOAD_FAILED);
            assertThat(document.getUploadError()).isEqualTo("Storage service unavailable");
        }
    }

    @Nested
    @DisplayName("Document Expiry Tests")
    class ExpiryTests {

        @Test
        @DisplayName("Should check if document is expired")
        void shouldCheckIfDocumentIsExpired() {
            document.setExpiryDate(LocalDateTime.now().minusDays(1));
            assertThat(document.isExpired()).isTrue();

            document.setExpiryDate(LocalDateTime.now().plusDays(30));
            assertThat(document.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should check if document is expiring soon (within 30 days)")
        void shouldCheckIfExpiringSoon() {
            document.setExpiryDate(LocalDateTime.now().plusDays(15));
            assertThat(document.isExpiringSoon()).isTrue();

            document.setExpiryDate(LocalDateTime.now().plusDays(60));
            assertThat(document.isExpiringSoon()).isFalse();
        }

        @Test
        @DisplayName("Should set default expiry based on document type")
        void shouldSetDefaultExpiryBasedOnType() {
            // PAN Card doesn't expire
            document.setDocumentType(DocumentType.PAN_CARD);
            document.setDefaultExpiry();
            assertThat(document.getExpiryDate()).isNull();

            // Salary slip expires in 3 months
            document.setDocumentType(DocumentType.SALARY_SLIP);
            document.setDefaultExpiry();
            assertThat(document.getExpiryDate()).isAfter(LocalDateTime.now().plusDays(80));
            assertThat(document.getExpiryDate()).isBefore(LocalDateTime.now().plusDays(100));
        }
    }

    @Nested
    @DisplayName("Version Control Tests")
    class VersionTests {

        @Test
        @DisplayName("Should start with version 1")
        void shouldStartWithVersion1() {
            assertThat(document.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should increment version on reupload")
        void shouldIncrementVersionOnReupload() {
            document.resetForReupload();
            assertThat(document.getVersion()).isEqualTo(2);

            document.resetForReupload();
            assertThat(document.getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should track previous version reference")
        void shouldTrackPreviousVersionReference() {
            String originalDocId = document.getId();
            document.setId("doc-001");

            Document newVersion = document.createNewVersion("new_pan.pdf", 2048000L);

            assertThat(newVersion.getPreviousVersionId()).isEqualTo("doc-001");
            assertThat(newVersion.getVersion()).isEqualTo(2);
            assertThat(newVersion.getOriginalFileName()).isEqualTo("new_pan.pdf");
        }
    }
}
