package com.loanflow.document.controller;

import com.loanflow.document.service.AuditEventService;
import com.loanflow.dto.audit.AuditEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TDD unit tests for AuditController (US-030).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditController Tests")
class AuditControllerTest {

    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private AuditController controller;

    private UUID applicationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("POST /events")
    class RecordEventTests {

        @Test
        @DisplayName("Should record audit event and return 201 CREATED")
        void shouldRecordEvent() {
            var input = AuditEventDto.builder()
                    .eventType("APPLICATION_CREATED")
                    .applicationId(applicationId)
                    .description("Test event")
                    .timestamp(Instant.now())
                    .build();

            var saved = AuditEventDto.builder()
                    .id("audit-001")
                    .eventType("APPLICATION_CREATED")
                    .applicationId(applicationId)
                    .description("Test event")
                    .timestamp(Instant.now())
                    .build();

            when(auditEventService.recordEvent(any(AuditEventDto.class))).thenReturn(saved);

            var result = controller.recordEvent(input);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData().getId()).isEqualTo("audit-001");
        }
    }

    @Nested
    @DisplayName("GET /application/{appId}")
    class GetByApplicationTests {

        @Test
        @DisplayName("Should return paginated events for application")
        void shouldReturnEvents() {
            var event = AuditEventDto.builder()
                    .id("e1")
                    .eventType("APPLICATION_SUBMITTED")
                    .applicationId(applicationId)
                    .timestamp(Instant.now())
                    .build();

            Page<AuditEventDto> page = new PageImpl<>(List.of(event), PageRequest.of(0, 50), 1);
            when(auditEventService.getByApplicationId(any(UUID.class), any()))
                    .thenReturn(page);

            var result = controller.getByApplicationId(applicationId, PageRequest.of(0, 50));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("GET /user/{userId}")
    class GetByUserTests {

        @Test
        @DisplayName("Should return events performed by specific user")
        void shouldReturnUserEvents() {
            var event = AuditEventDto.builder()
                    .id("e1")
                    .eventType("APPROVAL_GRANTED")
                    .performedBy(userId)
                    .timestamp(Instant.now())
                    .build();

            Page<AuditEventDto> page = new PageImpl<>(List.of(event), PageRequest.of(0, 50), 1);
            when(auditEventService.getByUserId(any(UUID.class), any()))
                    .thenReturn(page);

            var result = controller.getByUserId(userId, PageRequest.of(0, 50));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);
        }
    }
}
