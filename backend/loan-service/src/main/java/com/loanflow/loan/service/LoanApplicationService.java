package com.loanflow.loan.service;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.dto.CustomerLoanApplicationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoanApplicationService {

    /**
     * Create a new loan application
     */
    LoanApplicationResponse create(LoanApplicationRequest request);

    /**
     * Get loan application by ID
     */
    LoanApplicationResponse getById(UUID id);

    /**
     * Get loan application by application number
     */
    LoanApplicationResponse getByApplicationNumber(String applicationNumber);

    /**
     * Get all loan applications with pagination
     */
    Page<LoanApplicationResponse> getAll(Pageable pageable);

    /**
     * Get loan applications by customer ID
     */
    Page<LoanApplicationResponse> getByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Get loan applications by status
     */
    Page<LoanApplicationResponse> getByStatus(LoanStatus status, Pageable pageable);

    /**
     * Get loan applications assigned to an officer
     */
    Page<LoanApplicationResponse> getByAssignedOfficer(UUID officerId, Pageable pageable);

    /**
     * Update loan application (only in DRAFT or RETURNED status)
     */
    LoanApplicationResponse update(UUID id, LoanApplicationRequest request);

    /**
     * Submit loan application for processing
     */
    LoanApplicationResponse submit(UUID id);

    /**
     * Approve loan application
     */
    LoanApplicationResponse approve(UUID id, BigDecimal approvedAmount, BigDecimal interestRate);

    /**
     * Conditionally approve loan application
     */
    LoanApplicationResponse conditionallyApprove(UUID id, BigDecimal approvedAmount,
                                                  BigDecimal interestRate, String conditions);

    /**
     * Reject loan application
     */
    LoanApplicationResponse reject(UUID id, String reason);

    /**
     * Return application for correction
     */
    LoanApplicationResponse returnForCorrection(UUID id, String reason);

    /**
     * Cancel loan application
     */
    void cancel(UUID id, String reason);

    /**
     * Assign loan officer to application
     */
    LoanApplicationResponse assignOfficer(UUID id, UUID officerId);

    /**
     * Transition application status
     */
    LoanApplicationResponse transitionStatus(UUID id, LoanStatus newStatus);

    /**
     * Update CIBIL score
     */
    LoanApplicationResponse updateCibilScore(UUID id, Integer cibilScore, String riskCategory);

    /**
     * Search loan applications by application number
     */
    Page<LoanApplicationResponse> searchByApplicationNumber(String query, Pageable pageable);

    // ==================== CUSTOMER PORTAL METHODS ====================
    // Issue: #26 [US-024] Customer Loan Application Form

    /**
     * Get loan applications by customer email (for Customer Portal)
     */
    Page<LoanApplicationResponse> getByCustomerEmail(String email, Pageable pageable);

    /**
     * Create a loan application submitted by customer through portal
     */
    LoanApplicationResponse createCustomerApplication(String customerEmail, CustomerLoanApplicationRequest request);

    /**
     * Accept loan offer (Customer Portal)
     */
    LoanApplicationResponse acceptOffer(UUID applicationId, String customerEmail);

    /**
     * Reject loan offer (Customer Portal)
     */
    LoanApplicationResponse rejectOffer(UUID applicationId, String customerEmail, String reason);
}
