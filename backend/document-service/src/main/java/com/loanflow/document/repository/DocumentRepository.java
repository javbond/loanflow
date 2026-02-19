package com.loanflow.document.repository;

import com.loanflow.document.domain.entity.Document;
import com.loanflow.document.domain.enums.DocumentCategory;
import com.loanflow.document.domain.enums.DocumentStatus;
import com.loanflow.document.domain.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MongoDB repository for Document entities
 */
@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    Optional<Document> findByDocumentNumber(String documentNumber);

    Page<Document> findByApplicationId(UUID applicationId, Pageable pageable);

    Page<Document> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Document> findByApplicationIdAndStatus(UUID applicationId, DocumentStatus status, Pageable pageable);

    Page<Document> findByApplicationIdAndCategory(UUID applicationId, DocumentCategory category, Pageable pageable);

    List<Document> findByApplicationIdAndDocumentType(UUID applicationId, DocumentType documentType);

    long countByApplicationId(UUID applicationId);

    long countByApplicationIdAndStatus(UUID applicationId, DocumentStatus status);

    @Query(value = "{ 'applicationId': ?0 }", fields = "{ 'documentType': 1 }")
    List<DocumentType> findDistinctDocumentTypesByApplicationId(UUID applicationId);

    List<Document> findByApplicationIdAndStatusIn(UUID applicationId, List<DocumentStatus> statuses);

    @Query("{ 'expiryDate': { $lt: ?0 }, 'status': { $ne: 'EXPIRED' } }")
    List<Document> findExpiredDocuments(LocalDateTime now);

    @Query("{ 'expiryDate': { $gte: ?0, $lt: ?1 }, 'status': 'VERIFIED' }")
    List<Document> findDocumentsExpiringSoon(LocalDateTime from, LocalDateTime to);

    boolean existsByApplicationIdAndDocumentType(UUID applicationId, DocumentType documentType);

    @Query(value = "{ 'applicationId': ?0 }", count = true)
    long countDocumentsByApplication(UUID applicationId);

    void deleteByApplicationId(UUID applicationId);
}
