package com.loanflow.loan.workflow.assignment;

import java.util.Optional;

/**
 * Strategy interface for selecting which officer to assign a workflow task to.
 *
 * Implementations choose an officer from the configured list for a given candidate group.
 * Returns Optional.empty() if no officers are available for the group.
 */
public interface AssignmentStrategy {

    /**
     * Select an officer to assign a task to, based on the candidate group.
     *
     * @param candidateGroup the Flowable candidate group name (e.g., "LOAN_OFFICER")
     * @return the user ID (Keycloak subject UUID) of the selected officer, or empty if none available
     */
    Optional<String> selectAssignee(String candidateGroup);
}
