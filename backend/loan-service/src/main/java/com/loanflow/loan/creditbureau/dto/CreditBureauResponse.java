package com.loanflow.loan.creditbureau.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO from CIBIL credit bureau pull.
 * Contains credit score, account summaries, enquiry history, and negative markers.
 * Cached in Redis with 24h TTL to avoid duplicate bureau charges.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauResponse implements Serializable {

    private String pan;
    private int creditScore;                    // 300-900 (CIBIL range)
    private String scoreVersion;                // e.g., "CIBIL TransUnion Score 2.0"

    @Builder.Default
    private List<String> scoreFactors = new ArrayList<>();    // Factors affecting score

    @Builder.Default
    private List<AccountSummary> accounts = new ArrayList<>();
    @Builder.Default
    private List<EnquirySummary> enquiries = new ArrayList<>();

    private int dpd90PlusCount;                 // Accounts with 90+ DPD
    private int writtenOffAccounts;             // Written-off accounts
    private int enquiryCount30Days;             // Hard enquiries in last 30 days
    private int totalActiveAccounts;
    private double totalOutstandingBalance;

    private BureauDataSource dataSource;        // REAL, CACHED, SIMULATED
    private Instant pullTimestamp;
    private String controlNumber;               // CIBIL report reference number
}
