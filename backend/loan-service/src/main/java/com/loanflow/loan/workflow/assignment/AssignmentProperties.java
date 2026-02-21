package com.loanflow.loan.workflow.assignment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for task assignment and SLA monitoring.
 *
 * Binds to the "loanflow.assignment" YAML block in application.yml.
 * Controls auto-assignment strategy, officer lists per role, and SLA timeouts.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "loanflow.assignment")
public class AssignmentProperties {

    /**
     * Whether auto-assignment is enabled. When false, tasks remain in candidate group pool.
     */
    private boolean enabled = true;

    /**
     * Assignment strategy to use: ROUND_ROBIN or WORKLOAD_BASED.
     */
    private Strategy strategy = Strategy.ROUND_ROBIN;

    /**
     * Whether SLA monitoring and automatic escalation is enabled.
     */
    private boolean slaEnabled = true;

    /**
     * Interval in milliseconds between SLA check runs (default: 5 minutes).
     */
    private long slaCheckIntervalMs = 300000;

    /**
     * Map of candidate group name -> list of officer user IDs (Keycloak subject UUIDs).
     * Example: LOAN_OFFICER -> [uuid-1, uuid-2, uuid-3]
     */
    private Map<String, List<String>> officers = new HashMap<>();

    /**
     * SLA configuration per task definition key.
     * Example: documentVerification -> {timeoutHours: 24, escalateTo: SENIOR_UNDERWRITER}
     */
    private Map<String, SlaConfig> sla = new HashMap<>();

    /**
     * Available assignment strategies.
     */
    public enum Strategy {
        ROUND_ROBIN,
        WORKLOAD_BASED
    }

    /**
     * SLA configuration for a specific task type.
     */
    @Data
    public static class SlaConfig {
        /**
         * Maximum hours allowed before the task is considered SLA-breached.
         */
        private int timeoutHours = 24;

        /**
         * Candidate group name to escalate to when SLA is breached.
         */
        private String escalateTo;
    }
}
