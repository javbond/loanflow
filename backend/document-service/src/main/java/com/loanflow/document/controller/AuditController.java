package com.loanflow.document.controller;

import com.loanflow.document.service.AuditEventService;
import com.loanflow.dto.audit.AuditEventDto;
import com.loanflow.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit trail operations (US-030).
 * Provides endpoints for querying and recording audit events.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Trail", description = "Audit trail APIs for regulatory compliance")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditEventService auditEventService;

    @PostMapping("/events")
    @Operation(summary = "Record audit event",
            description = "Record a new audit event from a microservice (internal API)")
    public ResponseEntity<ApiResponse<AuditEventDto>> recordEvent(
            @Valid @RequestBody AuditEventDto event) {

        log.debug("Recording audit event: type={}, applicationId={}",
                event.getEventType(), event.getApplicationId());
        AuditEventDto saved = auditEventService.recordEvent(event);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audit event recorded", saved));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    @Operation(summary = "Get audit trail for loan application",
            description = "Returns all audit events for a loan application in chronological order")
    public ResponseEntity<ApiResponse<Page<AuditEventDto>>> getByApplicationId(
            @PathVariable UUID applicationId,
            @PageableDefault(size = 50) Pageable pageable) {

        log.debug("Fetching audit trail for application {}", applicationId);
        Page<AuditEventDto> events = auditEventService.getByApplicationId(applicationId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Retrieved %d audit events", events.getTotalElements()), events));
    }

    @GetMapping("/application/{applicationId}/filter")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER', 'ADMIN')")
    @Operation(summary = "Filter audit events",
            description = "Filter audit events by event type and/or date range")
    public ResponseEntity<ApiResponse<List<AuditEventDto>>> filterEvents(
            @PathVariable UUID applicationId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        log.debug("Filtering audit events for application {}: type={}, from={}, to={}",
                applicationId, eventType, from, to);

        List<AuditEventDto> events;

        if (eventType != null && from != null && to != null) {
            events = auditEventService.getByApplicationAndEventTypeAndDateRange(
                    applicationId, eventType, from, to);
        } else if (eventType != null) {
            events = auditEventService.getByApplicationAndEventType(applicationId, eventType);
        } else if (from != null && to != null) {
            events = auditEventService.getByApplicationAndDateRange(applicationId, from, to);
        } else {
            // No filter — return all paginated
            events = auditEventService.getByApplicationId(
                    applicationId, Pageable.unpaged()).getContent();
        }

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d matching events", events.size()), events));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get audit events by user",
            description = "Returns all actions performed by a specific user")
    public ResponseEntity<ApiResponse<Page<AuditEventDto>>> getByUserId(
            @PathVariable UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {

        log.debug("Fetching audit trail for user {}", userId);
        Page<AuditEventDto> events = auditEventService.getByUserId(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Retrieved %d audit events", events.getTotalElements()), events));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get audit events by customer",
            description = "Returns all audit events related to a customer")
    public ResponseEntity<ApiResponse<Page<AuditEventDto>>> getByCustomerId(
            @PathVariable UUID customerId,
            @PageableDefault(size = 50) Pageable pageable) {

        log.debug("Fetching audit trail for customer {}", customerId);
        Page<AuditEventDto> events = auditEventService.getByCustomerId(customerId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Retrieved %d audit events", events.getTotalElements()), events));
    }
}
