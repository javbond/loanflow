package com.loanflow.document.service;

import com.loanflow.document.domain.entity.AuditEvent;
import com.loanflow.document.repository.AuditEventRepository;
import com.loanflow.dto.audit.AuditEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for AuditEventService (US-030).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventService Tests")
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository repository;

    @InjectMocks
    private AuditEventService service;

    private UUID applicationId;
    private UUID userId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("recordEvent")
    class RecordEventTests {

        @Test
        @DisplayName("Should save audit event and return DTO with generated ID")
        void shouldSaveAndReturnAuditEvent() {
            AuditEventDto input = AuditEventDto.builder()
                    .eventType("APPLICATION_CREATED")
                    .description("New loan application created")
                    .serviceName("loan-service")
                    .resourceType("LoanApplication")
                    .applicationId(applicationId)
                    .performedBy(userId)
                    .performedByName("officer@loanflow.com")
                    .performedByRole("LOAN_OFFICER")
                    .timestamp(Instant.now())
                    .build();

            AuditEvent savedEntity = AuditEvent.builder()
                    .id("audit-123")
                    .eventType("APPLICATION_CREATED")
                    .description("New loan application created")
                    .serviceName("loan-service")
                    .resourceType("LoanApplication")
                    .applicationId(applicationId)
                    .performedBy(userId)
                    .performedByName("officer@loanflow.com")
                    .performedByRole("LOAN_OFFICER")
                    .timestamp(Instant.now())
                    .build();

            when(repository.save(any(AuditEvent.class))).thenReturn(savedEntity);

            AuditEventDto result = service.recordEvent(input);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("audit-123");
            assertThat(result.getEventType()).isEqualTo("APPLICATION_CREATED");
            assertThat(result.getApplicationId()).isEqualTo(applicationId);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("APPLICATION_CREATED");
        }

        @Test
        @DisplayName("Should preserve before/after state in audit event")
        void shouldPreserveStateInAuditEvent() {
            Map<String, Object> beforeState = Map.of("status", "DRAFT");
            Map<String, Object> afterState = Map.of("status", "SUBMITTED");

            AuditEventDto input = AuditEventDto.builder()
                    .eventType("STATUS_CHANGED")
                    .applicationId(applicationId)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .timestamp(Instant.now())
                    .build();

            AuditEvent savedEntity = AuditEvent.builder()
                    .id("audit-456")
                    .eventType("STATUS_CHANGED")
                    .applicationId(applicationId)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .timestamp(Instant.now())
                    .build();

            when(repository.save(any(AuditEvent.class))).thenReturn(savedEntity);

            AuditEventDto result = service.recordEvent(input);

            assertThat(result.getBeforeState()).containsEntry("status", "DRAFT");
            assertThat(result.getAfterState()).containsEntry("status", "SUBMITTED");
        }
    }

    @Nested
    @DisplayName("getByApplicationId")
    class GetByApplicationIdTests {

        @Test
        @DisplayName("Should return paginated audit events for application")
        void shouldReturnPaginatedEvents() {
            AuditEvent event1 = AuditEvent.builder()
                    .id("e1").eventType("APPLICATION_CREATED")
                    .applicationId(applicationId)
                    .timestamp(Instant.now().minusSeconds(3600))
                    .build();
            AuditEvent event2 = AuditEvent.builder()
                    .id("e2").eventType("APPLICATION_SUBMITTED")
                    .applicationId(applicationId)
                    .timestamp(Instant.now())
                    .build();

            Page<AuditEvent> page = new PageImpl<>(List.of(event2, event1), PageRequest.of(0, 50), 2);
            when(repository.findByApplicationIdOrderByTimestampDesc(applicationId, PageRequest.of(0, 50)))
                    .thenReturn(page);

            Page<AuditEventDto> result = service.getByApplicationId(applicationId, PageRequest.of(0, 50));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getEventType()).isEqualTo("APPLICATION_SUBMITTED");
        }
    }

    @Nested
    @DisplayName("getByUserId")
    class GetByUserIdTests {

        @Test
        @DisplayName("Should return audit events for a specific user")
        void shouldReturnEventsForUser() {
            AuditEvent event = AuditEvent.builder()
                    .id("e1").eventType("APPROVAL_GRANTED")
                    .applicationId(applicationId)
                    .performedBy(userId)
                    .performedByName("officer@loanflow.com")
                    .timestamp(Instant.now())
                    .build();

            Page<AuditEvent> page = new PageImpl<>(List.of(event), PageRequest.of(0, 50), 1);
            when(repository.findByPerformedByOrderByTimestampDesc(userId, PageRequest.of(0, 50)))
                    .thenReturn(page);

            Page<AuditEventDto> result = service.getByUserId(userId, PageRequest.of(0, 50));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getPerformedByName()).isEqualTo("officer@loanflow.com");
        }
    }

    @Nested
    @DisplayName("Filtering")
    class FilterTests {

        @Test
        @DisplayName("Should filter events by application and event type")
        void shouldFilterByEventType() {
            AuditEvent event = AuditEvent.builder()
                    .id("e1").eventType("APPROVAL_GRANTED")
                    .applicationId(applicationId)
                    .timestamp(Instant.now())
                    .build();

            when(repository.findByApplicationIdAndEventTypeOrderByTimestampDesc(
                    applicationId, "APPROVAL_GRANTED"))
                    .thenReturn(List.of(event));

            List<AuditEventDto> result = service.getByApplicationAndEventType(
                    applicationId, "APPROVAL_GRANTED");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).isEqualTo("APPROVAL_GRANTED");
        }

        @Test
        @DisplayName("Should filter events by date range")
        void shouldFilterByDateRange() {
            Instant from = Instant.parse("2026-02-01T00:00:00Z");
            Instant to = Instant.parse("2026-02-28T23:59:59Z");

            AuditEvent event = AuditEvent.builder()
                    .id("e1").eventType("STATUS_CHANGED")
                    .applicationId(applicationId)
                    .timestamp(Instant.parse("2026-02-15T10:30:00Z"))
                    .build();

            when(repository.findByApplicationIdAndTimestampBetween(applicationId, from, to))
                    .thenReturn(List.of(event));

            List<AuditEventDto> result = service.getByApplicationAndDateRange(applicationId, from, to);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should count events for application")
        void shouldCountEvents() {
            when(repository.countByApplicationId(applicationId)).thenReturn(5L);

            long count = service.countByApplicationId(applicationId);

            assertThat(count).isEqualTo(5);
        }
    }
}
