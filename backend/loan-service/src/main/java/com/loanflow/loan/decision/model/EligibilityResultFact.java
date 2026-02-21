package com.loanflow.loan.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drools fact for eligibility determination output.
 * Inserted empty into working memory; rules populate it with results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResultFact {

    private String applicationId;

    /** Current eligibility status — set by rules */
    private EligibilityStatus status;

    /** Map of rejection code -> reason message */
    @Builder.Default
    private Map<String, String> rejectionReasons = new LinkedHashMap<>();

    /** Reason for referral to senior (if status == REFER) */
    private String referReason;

    /** Pending KYC items */
    @Builder.Default
    private Map<String, String> pendingItems = new LinkedHashMap<>();

    /** Maximum eligible loan amount (calculated by rules) */
    private double maxEligibleAmount;

    /** Maximum eligible tenure */
    private int maxEligibleTenure;

    /** FOIR percentage (calculated by rules) */
    private double foirPercentage;

    /** Recommended interest rate from eligibility rules */
    private double recommendedInterestRate;

    // ---- Methods called by DRL rules ----

    public void addRejectionReason(String code, String reason) {
        if (rejectionReasons == null) {
            rejectionReasons = new LinkedHashMap<>();
        }
        rejectionReasons.put(code, reason);
    }

    public void addPendingItem(String code, String description) {
        if (pendingItems == null) {
            pendingItems = new LinkedHashMap<>();
        }
        pendingItems.put(code, description);
    }

    /**
     * Convenience method for DRL: check if rejected
     */
    public boolean isRejected() {
        return status == EligibilityStatus.REJECTED;
    }

    /**
     * Get rejection reasons as a simple list of reason strings
     */
    public List<String> getRejectionReasonList() {
        if (rejectionReasons == null || rejectionReasons.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(rejectionReasons.values());
    }

    /**
     * Alias used by DRL: getRejectionReasons() returns the map.
     * DRL checks rejectionReasons.isEmpty()
     */
    public Map<String, String> getRejectionReasons() {
        if (rejectionReasons == null) {
            rejectionReasons = new LinkedHashMap<>();
        }
        return rejectionReasons;
    }

    /**
     * Alias used by DRL: setStatus
     */
    public void setStatus(EligibilityStatus status) {
        this.status = status;
    }
}
