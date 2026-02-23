package com.loanflow.document.repository;

import com.loanflow.document.domain.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB repository for AuditEvent documents (US-030).
 */
@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    /**
     * Find all audit events for a loan application, ordered by timestamp descending.
     */
    Page<AuditEvent> findByApplicationIdOrderByTimestampDesc(UUID applicationId, Pageable pageable);

    /**
     * Find all audit events performed by a specific user.
     */
    Page<AuditEvent> findByPerformedByOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find audit events by application ID and event type.
     */
    List<AuditEvent> findByApplicationIdAndEventTypeOrderByTimestampDesc(
            UUID applicationId, String eventType);

    /**
     * Find audit events by application ID within a date range.
     */
    @Query("{ 'application_id': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<AuditEvent> findByApplicationIdAndTimestampBetween(
            UUID applicationId, Instant from, Instant to);

    /**
     * Find audit events by application ID, event type, and date range.
     */
    @Query("{ 'application_id': ?0, 'event_type': ?1, 'timestamp': { $gte: ?2, $lte: ?3 } }")
    List<AuditEvent> findByApplicationIdAndEventTypeAndTimestampBetween(
            UUID applicationId, String eventType, Instant from, Instant to);

    /**
     * Find events by customer ID.
     */
    Page<AuditEvent> findByCustomerIdOrderByTimestampDesc(UUID customerId, Pageable pageable);

    /**
     * Count events for an application.
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Find events by service name (useful for debugging).
     */
    List<AuditEvent> findByServiceNameOrderByTimestampDesc(String serviceName);
}
