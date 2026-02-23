package com.loanflow.loan.audit;

import com.loanflow.dto.audit.AuditEventDto;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.util.audit.Auditable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for AuditAspect (US-030).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditAspect Tests")
class AuditAspectTest {

    @Mock
    private AuditClient auditClient;

    @InjectMocks
    private AuditAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should publish audit event after successful method execution")
    void shouldPublishAuditEventOnSuccess() throws Throwable {
        // Arrange
        Auditable auditable = mock(Auditable.class);
        when(auditable.eventType()).thenReturn("APPLICATION_CREATED");
        when(auditable.description()).thenReturn("New loan application created");
        when(auditable.resourceType()).thenReturn("LoanApplication");

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(applicationId);
        response.setApplicationNumber("LN-2026-000001");
        response.setStatus("DRAFT");
        response.setLoanType("HOME_LOAN");

        when(joinPoint.proceed()).thenReturn(response);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"request"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{new Object()});

        // Act
        Object result = aspect.auditMethod(joinPoint, auditable);

        // Assert
        assertThat(result).isEqualTo(response);

        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditClient).publishEvent(captor.capture());

        AuditEventDto event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("APPLICATION_CREATED");
        assertThat(event.getDescription()).isEqualTo("New loan application created");
        assertThat(event.getResourceType()).isEqualTo("LoanApplication");
        assertThat(event.getApplicationId()).isEqualTo(applicationId);
        assertThat(event.getServiceName()).isEqualTo("loan-service");
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should capture after state from LoanApplicationResponse")
    void shouldCaptureAfterState() throws Throwable {
        Auditable auditable = mock(Auditable.class);
        when(auditable.eventType()).thenReturn("APPROVAL_GRANTED");
        when(auditable.description()).thenReturn("Loan approved");
        when(auditable.resourceType()).thenReturn("LoanApplication");

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(applicationId);
        response.setApplicationNumber("LN-2026-000002");
        response.setStatus("APPROVED");
        response.setApprovedAmount(BigDecimal.valueOf(5000000));
        response.setInterestRate(BigDecimal.valueOf(10.5));

        when(joinPoint.proceed()).thenReturn(response);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{applicationId});

        aspect.auditMethod(joinPoint, auditable);

        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditClient).publishEvent(captor.capture());

        AuditEventDto event = captor.getValue();
        assertThat(event.getAfterState()).containsEntry("status", "APPROVED");
        assertThat(event.getAfterState()).containsEntry("applicationNumber", "LN-2026-000002");
        assertThat(event.getAfterState()).containsEntry("approvedAmount", "5000000");
    }

    @Test
    @DisplayName("Should not fail business operation when audit publishing fails")
    void shouldNotFailOnAuditError() throws Throwable {
        Auditable auditable = mock(Auditable.class);
        when(auditable.eventType()).thenReturn("APPLICATION_SUBMITTED");
        when(auditable.description()).thenReturn("Submitted");
        when(auditable.resourceType()).thenReturn("LoanApplication");

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(applicationId);
        response.setStatus("SUBMITTED");

        when(joinPoint.proceed()).thenReturn(response);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        doThrow(new RuntimeException("Connection refused"))
                .when(auditClient).publishEvent(any());

        // Should NOT throw — audit failure must not break business logic
        Object result = aspect.auditMethod(joinPoint, auditable);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("Should extract application ID from method arguments when result has no ID")
    void shouldExtractAppIdFromArguments() throws Throwable {
        Auditable auditable = mock(Auditable.class);
        when(auditable.eventType()).thenReturn("STATUS_CHANGED");
        when(auditable.description()).thenReturn("Status change");
        when(auditable.resourceType()).thenReturn("");

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setStatus("UNDERWRITING");
        // No ID set on response

        when(joinPoint.proceed()).thenReturn(response);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id", "newStatus"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{applicationId, "UNDERWRITING"});

        aspect.auditMethod(joinPoint, auditable);

        ArgumentCaptor<AuditEventDto> captor = ArgumentCaptor.forClass(AuditEventDto.class);
        verify(auditClient).publishEvent(captor.capture());

        assertThat(captor.getValue().getApplicationId()).isEqualTo(applicationId);
        // Default resourceType when empty
        assertThat(captor.getValue().getResourceType()).isEqualTo("LoanApplication");
    }
}
