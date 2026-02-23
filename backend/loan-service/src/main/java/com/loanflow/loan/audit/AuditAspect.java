package com.loanflow.loan.audit;

import com.loanflow.dto.audit.AuditEventDto;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.security.util.SecurityUtils;
import com.loanflow.util.audit.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spring AOP Aspect for automatic audit logging (US-030).
 * Intercepts methods annotated with @Auditable and records before/after state.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // Capture before state from method arguments
        Map<String, Object> beforeState = captureBeforeState(joinPoint);

        // Execute the target method
        Object result = joinPoint.proceed();

        // Capture after state from result
        Map<String, Object> afterState = captureAfterState(result);

        // Build and publish audit event asynchronously
        try {
            AuditEventDto event = buildAuditEvent(auditable, joinPoint, beforeState, afterState, result);
            auditClient.publishEvent(event);
        } catch (Exception e) {
            // Audit must never fail the business operation
            log.warn("Failed to build/publish audit event for {}: {}",
                    joinPoint.getSignature().getName(), e.getMessage());
        }

        return result;
    }

    private AuditEventDto buildAuditEvent(Auditable auditable, ProceedingJoinPoint joinPoint,
                                           Map<String, Object> beforeState,
                                           Map<String, Object> afterState,
                                           Object result) {
        UUID applicationId = extractApplicationId(joinPoint, result);
        UUID customerId = extractCustomerId(joinPoint, result);
        String description = auditable.description().isEmpty()
                ? generateDescription(auditable.eventType(), joinPoint)
                : auditable.description();

        // Get user info from security context
        UUID performedBy = SecurityUtils.getCurrentUserId().orElse(null);
        String performedByName = SecurityUtils.getCurrentUserFullName().orElse(
                SecurityUtils.getCurrentUsername().orElse("system"));
        Set<String> roles = SecurityUtils.getCurrentUserRoles();
        String performedByRole = roles.isEmpty() ? "SYSTEM" : String.join(",", roles);

        return AuditEventDto.builder()
                .eventType(auditable.eventType())
                .description(description)
                .serviceName("loan-service")
                .resourceType(auditable.resourceType().isEmpty() ? "LoanApplication" : auditable.resourceType())
                .resourceId(applicationId != null ? applicationId.toString() : null)
                .applicationId(applicationId)
                .customerId(customerId)
                .performedBy(performedBy)
                .performedByName(performedByName)
                .performedByRole(performedByRole)
                .beforeState(beforeState)
                .afterState(afterState)
                .timestamp(Instant.now())
                .build();
    }

    private Map<String, Object> captureBeforeState(ProceedingJoinPoint joinPoint) {
        Map<String, Object> state = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (args[i] != null) {
                    state.put(paramNames[i], summarizeArg(args[i]));
                }
            }
        }
        return state;
    }

    private Map<String, Object> captureAfterState(Object result) {
        Map<String, Object> state = new HashMap<>();
        if (result instanceof LoanApplicationResponse response) {
            state.put("applicationNumber", response.getApplicationNumber());
            state.put("status", response.getStatus());
            state.put("loanType", response.getLoanType());
            if (response.getApprovedAmount() != null) {
                state.put("approvedAmount", response.getApprovedAmount().toString());
            }
            if (response.getInterestRate() != null) {
                state.put("interestRate", response.getInterestRate().toString());
            }
            if (response.getCibilScore() != null) {
                state.put("cibilScore", response.getCibilScore());
            }
            if (response.getRiskCategory() != null) {
                state.put("riskCategory", response.getRiskCategory());
            }
        }
        return state;
    }

    private UUID extractApplicationId(ProceedingJoinPoint joinPoint, Object result) {
        // Try from result first
        if (result instanceof LoanApplicationResponse response && response.getId() != null) {
            return response.getId();
        }
        // Try from arguments
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("id".equals(paramNames[i]) && args[i] instanceof UUID uuid) {
                    return uuid;
                }
            }
        }
        return null;
    }

    private UUID extractCustomerId(ProceedingJoinPoint joinPoint, Object result) {
        if (result instanceof LoanApplicationResponse response && response.getCustomerId() != null) {
            return response.getCustomerId();
        }
        return null;
    }

    private String summarizeArg(Object arg) {
        if (arg instanceof UUID || arg instanceof String || arg instanceof Number) {
            return arg.toString();
        }
        return arg.getClass().getSimpleName();
    }

    private String generateDescription(String eventType, ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return eventType.replace("_", " ").toLowerCase() + " via " + methodName;
    }
}
