package com.loanflow.util.audit;

import java.lang.annotation.*;

/**
 * Annotation to mark service methods for automatic audit logging via AOP (US-030).
 * When a method annotated with @Auditable completes successfully,
 * an AuditEvent is recorded with before/after state.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * The type of audit event (e.g., APPLICATION_CREATED, STATUS_CHANGED).
     */
    String eventType();

    /**
     * Description of the action being performed.
     */
    String description() default "";

    /**
     * The resource type being acted upon (e.g., "LoanApplication", "Customer").
     */
    String resourceType() default "";
}
