package com.loanflow.loan.audit;

import com.loanflow.dto.audit.AuditEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for publishing audit events to document-service (US-030).
 * Sends audit events via REST to the centralized audit store in MongoDB.
 */
@Component
@Slf4j
public class AuditClient {

    private final RestTemplate restTemplate;
    private final String documentServiceUrl;

    public AuditClient(RestTemplate restTemplate,
                       @Value("${loanflow.services.document-url:http://localhost:8083}") String documentServiceUrl) {
        this.restTemplate = restTemplate;
        this.documentServiceUrl = documentServiceUrl;
    }

    /**
     * Publish an audit event to document-service asynchronously (fire-and-forget).
     * Audit logging must never block or fail the main business operation.
     */
    public void publishEvent(AuditEventDto event) {
        try {
            String url = documentServiceUrl + "/api/v1/audit/events";
            restTemplate.postForObject(url, event, Object.class);
            log.debug("Published audit event: type={}, applicationId={}",
                    event.getEventType(), event.getApplicationId());
        } catch (Exception e) {
            // Audit must not break the main business flow
            log.warn("Failed to publish audit event: type={}, error={}",
                    event.getEventType(), e.getMessage());
        }
    }
}
