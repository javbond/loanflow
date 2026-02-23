package com.loanflow.document.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document for audit trail events (US-030).
 * Stored in the "audit_events" collection for regulatory compliance traceability.
 */
@Document(collection = "audit_events")
@CompoundIndex(name = "app_event_idx", def = "{'application_id': 1, 'event_type': 1}")
@CompoundIndex(name = "app_timestamp_idx", def = "{'application_id': 1, 'timestamp': -1}")
@CompoundIndex(name = "user_timestamp_idx", def = "{'performed_by': 1, 'timestamp': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    @Field("event_type")
    private String eventType;

    @Field("description")
    private String description;

    @Indexed
    @Field("service_name")
    private String serviceName;

    @Field("resource_type")
    private String resourceType;

    @Field("resource_id")
    private String resourceId;

    @Indexed
    @Field("application_id")
    private UUID applicationId;

    @Indexed
    @Field("customer_id")
    private UUID customerId;

    @Indexed
    @Field("performed_by")
    private UUID performedBy;

    @Field("performed_by_name")
    private String performedByName;

    @Field("performed_by_role")
    private String performedByRole;

    @Field("before_state")
    private Map<String, Object> beforeState;

    @Field("after_state")
    private Map<String, Object> afterState;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("ip_address")
    private String ipAddress;

    @CreatedDate
    @Field("timestamp")
    private Instant timestamp;
}
