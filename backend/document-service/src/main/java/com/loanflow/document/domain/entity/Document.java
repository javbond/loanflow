package com.loanflow.document.domain.entity;

import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Document entity stored in MongoDB
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@CompoundIndex(name = "app_type_idx", def = "{'applicationId': 1, 'documentType': 1}")
@CompoundIndex(name = "app_status_idx", def = "{'applicationId': 1, 'status': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

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

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("document_number")
    private String documentNumber;

    @Indexed
    @Field("application_id")
    private UUID applicationId;

    @Indexed
    @Field("customer_id")
    private UUID customerId;

    @Indexed
    @Field("customer_email")
    private String customerEmail;

    @Field("document_type")
    private DocumentType documentType;

    @Field("category")
    private DocumentCategory category;

    @Field("original_file_name")
    private String originalFileName;

    @Field("content_type")
    private String contentType;

    @Field("file_size")
    private Long fileSize;

    @Field("status")
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Field("storage_bucket")
    private String storageBucket;

    @Field("storage_key")
    private String storageKey;

    @Field("checksum")
    private String checksum;

    @Field("upload_error")
    private String uploadError;

    @Field("uploaded_at")
    private LocalDateTime uploadedAt;

    @Field("uploaded_by")
    private UUID uploadedBy;

    @Field("verified_at")
    private LocalDateTime verifiedAt;

    @Field("verified_by")
    private UUID verifiedBy;

    @Field("verification_remarks")
    private String verificationRemarks;

    @Field("expiry_date")
    private LocalDateTime expiryDate;

    @Field("version_number")
    @Builder.Default
    private Integer version = 1;

    @Field("previous_version_id")
    private String previousVersionId;

    @Field("description")
    private String description;

    @Field("password_protected")
    @Builder.Default
    private Boolean passwordProtected = false;

    // OCR Extraction fields (US-022)
    @Field("extracted_data")
    private Map<String, String> extractedData;

    @Field("extraction_status")
    private String extractionStatus;

    @Field("extracted_text")
    private String extractedText;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Field("optimistic_lock_version")
    private Long lockVersion;

    // ==================== Builder Customization ====================

    public static class DocumentBuilder {
        public DocumentBuilder documentType(DocumentType type) {
            this.documentType = type;
            this.category = type != null ? type.getCategory() : null;
            return this;
        }
    }

    // ==================== Lifecycle Hooks ====================

    public void prePersist() {
        if (this.documentNumber == null) {
            this.documentNumber = generateDocumentNumber();
        }
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = DocumentStatus.PENDING;
        }
        if (this.category == null && this.documentType != null) {
            this.category = this.documentType.getCategory();
        }
    }

    // ==================== Business Methods ====================

    /**
     * Generate unique document number
     */
    public static String generateDocumentNumber() {
        long seq = SEQUENCE.incrementAndGet();
        return String.format("DOC-%d-%06d", Year.now().getValue(), seq);
    }

    /**
     * Verify the document
     */
    public void verify(UUID verifierId, String remarks) {
        if (this.status == DocumentStatus.VERIFIED) {
            throw new IllegalStateException("Document is already verified");
        }
        this.status = DocumentStatus.VERIFIED;
        this.verifiedBy = verifierId;
        this.verifiedAt = LocalDateTime.now();
        this.verificationRemarks = remarks;
    }

    /**
     * Reject the document
     */
    public void reject(UUID verifierId, String reason) {
        this.status = DocumentStatus.REJECTED;
        this.verifiedBy = verifierId;
        this.verifiedAt = LocalDateTime.now();
        this.verificationRemarks = reason;
    }

    /**
     * Check if document requires reupload
     */
    public boolean requiresReupload() {
        return status == DocumentStatus.REJECTED ||
                status == DocumentStatus.UPLOAD_FAILED ||
                status == DocumentStatus.EXPIRED;
    }

    /**
     * Reset document for reupload
     */
    public void resetForReupload() {
        this.status = DocumentStatus.PENDING;
        this.verifiedAt = null;
        this.verifiedBy = null;
        this.verificationRemarks = null;
        this.version++;
    }

    /**
     * Set storage location after upload
     */
    public void setStorageLocation(String bucket, String key) {
        this.storageBucket = bucket;
        this.storageKey = key;
        this.status = DocumentStatus.UPLOADED;
    }

    /**
     * Mark upload as failed
     */
    public void markUploadFailed(String error) {
        this.status = DocumentStatus.UPLOAD_FAILED;
        this.uploadError = error;
    }

    /**
     * Check if file type is allowed
     */
    public boolean isAllowedFileType() {
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Check if file size is within limit
     */
    public boolean isWithinSizeLimit() {
        return fileSize != null && fileSize <= MAX_FILE_SIZE;
    }

    /**
     * Get file extension
     */
    public String getFileExtension() {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Format file size for display
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Generate storage path
     */
    public String generateStoragePath() {
        String categoryFolder = category != null ? category.name() : "OTHER";
        String extension = getFileExtension();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("applications/%s/%s/%s_%s.%s",
                applicationId, categoryFolder, documentType, uniqueId, extension);
    }

    /**
     * Check if document is expired
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    /**
     * Check if document is expiring soon (within 30 days)
     */
    public boolean isExpiringSoon() {
        if (expiryDate == null) return false;
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        return expiryDate.isBefore(thirtyDaysFromNow) && !isExpired();
    }

    /**
     * Set default expiry based on document type
     */
    public void setDefaultExpiry() {
        if (documentType == null || !documentType.hasExpiry()) {
            this.expiryDate = null;
            return;
        }
        int months = documentType.getDefaultExpiryMonths();
        if (months > 0) {
            this.expiryDate = LocalDateTime.now().plusMonths(months);
        }
    }

    /**
     * Create a new version of this document
     */
    public Document createNewVersion(String newFileName, Long newFileSize) {
        return Document.builder()
                .applicationId(this.applicationId)
                .customerId(this.customerId)
                .documentType(this.documentType)
                .category(this.category)
                .originalFileName(newFileName)
                .fileSize(newFileSize)
                .version(this.version + 1)
                .previousVersionId(this.id)
                .status(DocumentStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
