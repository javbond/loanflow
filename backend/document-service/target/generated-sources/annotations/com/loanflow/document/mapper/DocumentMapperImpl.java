package com.loanflow.document.mapper;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.dto.response.DocumentResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T20:22:27+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 20.0.1 (Oracle Corporation)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public DocumentResponse toResponse(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentResponse.DocumentResponseBuilder documentResponse = DocumentResponse.builder();

        documentResponse.version( document.getVersion() );
        documentResponse.id( document.getId() );
        documentResponse.documentNumber( document.getDocumentNumber() );
        documentResponse.applicationId( document.getApplicationId() );
        documentResponse.customerId( document.getCustomerId() );
        documentResponse.originalFileName( document.getOriginalFileName() );
        documentResponse.contentType( document.getContentType() );
        documentResponse.fileSize( document.getFileSize() );
        documentResponse.storageBucket( document.getStorageBucket() );
        documentResponse.storageKey( document.getStorageKey() );
        documentResponse.uploadedAt( document.getUploadedAt() );
        documentResponse.uploadedBy( document.getUploadedBy() );
        documentResponse.verifiedAt( document.getVerifiedAt() );
        documentResponse.verifiedBy( document.getVerifiedBy() );
        documentResponse.verificationRemarks( document.getVerificationRemarks() );
        documentResponse.expiryDate( document.getExpiryDate() );
        documentResponse.previousVersionId( document.getPreviousVersionId() );
        documentResponse.createdAt( document.getCreatedAt() );
        documentResponse.updatedAt( document.getUpdatedAt() );

        documentResponse.documentType( document.getDocumentType() != null ? document.getDocumentType().name() : null );
        documentResponse.category( document.getCategory() != null ? document.getCategory().name() : null );
        documentResponse.status( document.getStatus() != null ? document.getStatus().name() : null );
        documentResponse.formattedFileSize( document.getFormattedFileSize() );
        documentResponse.expired( document.isExpired() );
        documentResponse.expiringSoon( document.isExpiringSoon() );

        return documentResponse.build();
    }
}
