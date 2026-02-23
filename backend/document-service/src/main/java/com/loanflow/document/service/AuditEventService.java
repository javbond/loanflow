package com.loanflow.document.service;

import com.loanflow.document.domain.entity.AuditEvent;
import com.loanflow.document.repository.AuditEventRepository;
import com.loanflow.dto.audit.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing audit events (US-030).
 * Provides CRUD and query operations for audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventService {

    private final AuditEventRepository repository;

    /**
     * Record a new audit event.
     */
    public AuditEventDto recordEvent(AuditEventDto dto) {
        AuditEvent entity = toEntity(dto);
        AuditEvent saved = repository.save(entity);
        log.debug("Recorded audit event: type={}, resource={}, applicationId={}",
                saved.getEventType(), saved.getResourceType(), saved.getApplicationId());
        return toDto(saved);
    }

    /**
     * Get all audit events for a loan application (paginated, newest first).
     */
    public Page<AuditEventDto> getByApplicationId(UUID applicationId, Pageable pageable) {
        return repository.findByApplicationIdOrderByTimestampDesc(applicationId, pageable)
                .map(this::toDto);
    }

    /**
     * Get all audit events performed by a user (paginated, newest first).
     */
    public Page<AuditEventDto> getByUserId(UUID userId, Pageable pageable) {
        return repository.findByPerformedByOrderByTimestampDesc(userId, pageable)
                .map(this::toDto);
    }

    /**
     * Get audit events for an application filtered by event type.
     */
    public List<AuditEventDto> getByApplicationAndEventType(UUID applicationId, String eventType) {
        return repository.findByApplicationIdAndEventTypeOrderByTimestampDesc(applicationId, eventType)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get audit events for an application within a date range.
     */
    public List<AuditEventDto> getByApplicationAndDateRange(UUID applicationId, Instant from, Instant to) {
        return repository.findByApplicationIdAndTimestampBetween(applicationId, from, to)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get audit events filtered by application, event type, and date range.
     */
    public List<AuditEventDto> getByApplicationAndEventTypeAndDateRange(
            UUID applicationId, String eventType, Instant from, Instant to) {
        return repository.findByApplicationIdAndEventTypeAndTimestampBetween(
                        applicationId, eventType, from, to)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get audit events for a customer (paginated).
     */
    public Page<AuditEventDto> getByCustomerId(UUID customerId, Pageable pageable) {
        return repository.findByCustomerIdOrderByTimestampDesc(customerId, pageable)
                .map(this::toDto);
    }

    /**
     * Count audit events for an application.
     */
    public long countByApplicationId(UUID applicationId) {
        return repository.countByApplicationId(applicationId);
    }

    // ==================== Mapping ====================

    private AuditEvent toEntity(AuditEventDto dto) {
        return AuditEvent.builder()
                .eventType(dto.getEventType())
                .description(dto.getDescription())
                .serviceName(dto.getServiceName())
                .resourceType(dto.getResourceType())
                .resourceId(dto.getResourceId())
                .applicationId(dto.getApplicationId())
                .customerId(dto.getCustomerId())
                .performedBy(dto.getPerformedBy())
                .performedByName(dto.getPerformedByName())
                .performedByRole(dto.getPerformedByRole())
                .beforeState(dto.getBeforeState())
                .afterState(dto.getAfterState())
                .metadata(dto.getMetadata())
                .ipAddress(dto.getIpAddress())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .build();
    }

    AuditEventDto toDto(AuditEvent entity) {
        return AuditEventDto.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .description(entity.getDescription())
                .serviceName(entity.getServiceName())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .applicationId(entity.getApplicationId())
                .customerId(entity.getCustomerId())
                .performedBy(entity.getPerformedBy())
                .performedByName(entity.getPerformedByName())
                .performedByRole(entity.getPerformedByRole())
                .beforeState(entity.getBeforeState())
                .afterState(entity.getAfterState())
                .metadata(entity.getMetadata())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
