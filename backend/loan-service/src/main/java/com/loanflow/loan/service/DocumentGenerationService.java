package com.loanflow.loan.service;

import java.util.UUID;

/**
 * Service interface for document generation (US-023).
 * Generates PDF documents (sanction letters, etc.) for approved loan applications.
 */
public interface DocumentGenerationService {

    /**
     * Generate a sanction letter PDF for an approved loan application.
     *
     * @param applicationId the loan application ID
     * @return PDF content as byte array
     * @throws IllegalStateException if the loan is not in an approved state
     */
    byte[] generateSanctionLetter(UUID applicationId);
}
