package com.loanflow.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for audit events exchanged between services (US-030).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Audit event record for regulatory compliance traceability")
public class AuditEventDto {

    @Schema(description = "Unique audit event ID")
    private String id;

    @Schema(description = "Event type", example = "APPLICATION_CREATED")
    private String eventType;

    @Schema(description = "Human-readable description", example = "Loan application submitted for processing")
    private String description;

    @Schema(description = "Service that generated the event", example = "loan-service")
    private String serviceName;

    @Schema(description = "Resource type", example = "LoanApplication")
    private String resourceType;

    @Schema(description = "Resource ID (e.g., loan application UUID)")
    private String resourceId;

    @Schema(description = "Loan application ID for cross-referencing")
    private UUID applicationId;

    @Schema(description = "Customer ID associated with the event")
    private UUID customerId;

    @Schema(description = "User ID who performed the action")
    private UUID performedBy;

    @Schema(description = "Username of the performer")
    private String performedByName;

    @Schema(description = "User's role at time of action")
    private String performedByRole;

    @Schema(description = "State before the action (JSON)")
    private Map<String, Object> beforeState;

    @Schema(description = "State after the action (JSON)")
    private Map<String, Object> afterState;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;

    @Schema(description = "IP address of the request")
    private String ipAddress;

    @Schema(description = "Timestamp of the event")
    private Instant timestamp;
}
