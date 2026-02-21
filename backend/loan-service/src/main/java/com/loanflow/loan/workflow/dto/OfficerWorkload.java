package com.loanflow.loan.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the workload of a single officer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfficerWorkload {

    /**
     * Keycloak subject UUID of the officer
     */
    private String userId;

    /**
     * Candidate group / role (e.g., "LOAN_OFFICER", "UNDERWRITER")
     */
    private String role;

    /**
     * Number of currently active (assigned) tasks
     */
    private long activeTaskCount;

    /**
     * Whether this officer has at least one task that has breached SLA
     */
    private boolean slaBreached;
}
