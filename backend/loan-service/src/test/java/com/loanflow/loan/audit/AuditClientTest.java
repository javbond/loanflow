package com.loanflow.loan.audit;

import com.loanflow.dto.audit.AuditEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for AuditClient (US-030).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditClient Tests")
class AuditClientTest {

    @Test
    @DisplayName("Should publish event to document-service via REST")
    void shouldPublishEventViaRest() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AuditClient client = new AuditClient(restTemplate, "http://localhost:8083");

        AuditEventDto event = AuditEventDto.builder()
                .eventType("APPLICATION_CREATED")
                .applicationId(UUID.randomUUID())
                .timestamp(Instant.now())
                .build();

        when(restTemplate.postForObject(anyString(), any(), eq(Object.class))).thenReturn(null);

        client.publishEvent(event);

        verify(restTemplate).postForObject(
                eq("http://localhost:8083/api/v1/audit/events"),
                eq(event),
                eq(Object.class));
    }

    @Test
    @DisplayName("Should not throw exception when REST call fails")
    void shouldNotThrowOnRestFailure() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AuditClient client = new AuditClient(restTemplate, "http://localhost:8083");

        AuditEventDto event = AuditEventDto.builder()
                .eventType("APPLICATION_SUBMITTED")
                .timestamp(Instant.now())
                .build();

        when(restTemplate.postForObject(anyString(), any(), eq(Object.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Should NOT throw
        assertThatCode(() -> client.publishEvent(event))
                .doesNotThrowAnyException();
    }
}
