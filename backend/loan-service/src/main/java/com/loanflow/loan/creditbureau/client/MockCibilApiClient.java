package com.loanflow.loan.creditbureau.client;

import com.loanflow.loan.creditbureau.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mock CIBIL API client for dev/uat/test environments.
 * Generates deterministic credit data based on PAN hash:
 * - Same PAN always returns the same credit score (reproducible testing)
 * - Score range: 300-900
 * - Generates realistic account summaries and enquiry records
 */
@Component
@Profile({"dev", "uat", "test", "default"})
@Slf4j
public class MockCibilApiClient implements CibilApiClient {

    @Override
    public CreditBureauResponse fetchCreditReport(CreditBureauRequest request) {
        log.info("Mock CIBIL: Generating credit report for PAN {}", maskPan(request.getPan()));

        // Deterministic score from PAN hash
        int score = generateDeterministicScore(request.getPan());
        int dpd90Plus = score < 550 ? 2 : (score < 650 ? 1 : 0);
        int writeOffs = score < 500 ? 1 : 0;
        int enquiries = generateEnquiryCount(request.getPan());

        // Generate mock accounts
        List<AccountSummary> accounts = generateAccounts(request.getPan(), score);
        List<EnquirySummary> enquiryList = generateEnquiries(request.getPan(), enquiries);
        List<String> scoreFactors = generateScoreFactors(score);

        double totalBalance = accounts.stream()
                .mapToDouble(AccountSummary::getCurrentBalance)
                .sum();
        long activeCount = accounts.stream()
                .filter(a -> "Active".equals(a.getAccountStatus()))
                .count();

        CreditBureauResponse response = CreditBureauResponse.builder()
                .pan(request.getPan())
                .creditScore(score)
                .scoreVersion("CIBIL TransUnion Score 2.0 (Mock)")
                .scoreFactors(scoreFactors)
                .accounts(accounts)
                .enquiries(enquiryList)
                .dpd90PlusCount(dpd90Plus)
                .writtenOffAccounts(writeOffs)
                .enquiryCount30Days(enquiries)
                .totalActiveAccounts((int) activeCount)
                .totalOutstandingBalance(totalBalance)
                .dataSource(BureauDataSource.REAL)  // Simulates the "real" client path
                .pullTimestamp(Instant.now())
                .controlNumber("MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        log.info("Mock CIBIL: PAN {} → Score={}, DPD90+={}, WriteOffs={}, Enquiries={}",
                maskPan(request.getPan()), score, dpd90Plus, writeOffs, enquiries);

        return response;
    }

    /**
     * Deterministic score from PAN hash.
     * Range: 300-900. Same PAN always returns the same score.
     */
    int generateDeterministicScore(String pan) {
        if (pan == null || pan.isEmpty()) return 700;
        int hash = Math.abs(pan.hashCode());
        return 300 + (hash % 601); // 300-900
    }

    private int generateEnquiryCount(String pan) {
        int hash = Math.abs(pan.hashCode());
        return (hash % 7); // 0-6
    }

    private List<AccountSummary> generateAccounts(String pan, int score) {
        List<AccountSummary> accounts = new ArrayList<>();
        int hash = Math.abs(pan.hashCode());
        int accountCount = 2 + (hash % 4); // 2-5 accounts

        String[] types = {"Personal Loan", "Credit Card", "Home Loan", "Vehicle Loan", "Gold Loan"};
        String[] lenders = {"HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Kotak Mahindra"};

        for (int i = 0; i < accountCount; i++) {
            boolean isActive = (hash + i) % 3 != 0; // ~67% active
            String dpdStatus = score >= 700 ? "000" : (score >= 550 ? "030" : "090+");

            accounts.add(AccountSummary.builder()
                    .accountType(types[(hash + i) % types.length])
                    .lenderName(lenders[(hash + i * 3) % lenders.length])
                    .currentBalance(isActive ? (50000.0 + ((hash + i * 7) % 500000)) : 0)
                    .amountOverdue(score < 650 && isActive ? ((hash + i) % 10000) : 0)
                    .dpdStatus(isActive ? dpdStatus : "000")
                    .accountStatus(isActive ? "Active" : "Closed")
                    .openDate(LocalDate.now().minusMonths(12 + ((hash + i * 5) % 60)))
                    .lastPaymentDate(isActive ? LocalDate.now().minusDays(1 + ((hash + i * 2) % 30)) : null)
                    .build());
        }
        return accounts;
    }

    private List<EnquirySummary> generateEnquiries(String pan, int count) {
        List<EnquirySummary> enquiries = new ArrayList<>();
        int hash = Math.abs(pan.hashCode());
        String[] purposes = {"Personal Loan", "Credit Card", "Home Loan", "Auto Loan"};
        String[] members = {"HDFC Bank", "SBI", "ICICI Bank", "Bajaj Finance", "Tata Capital"};

        for (int i = 0; i < Math.min(count, 5); i++) {
            enquiries.add(EnquirySummary.builder()
                    .enquiryDate(LocalDate.now().minusDays(1 + ((hash + i * 11) % 90)))
                    .memberName(members[(hash + i * 2) % members.length])
                    .purpose(purposes[(hash + i * 3) % purposes.length])
                    .amount(100000.0 + ((hash + i * 13) % 900000))
                    .build());
        }
        return enquiries;
    }

    private List<String> generateScoreFactors(int score) {
        List<String> factors = new ArrayList<>();
        if (score < 650) {
            factors.add("High credit utilization ratio");
            factors.add("Recent payment delays");
        }
        if (score < 550) {
            factors.add("Accounts in default");
        }
        if (score >= 700) {
            factors.add("Long credit history");
            factors.add("Low credit utilization");
        }
        if (score >= 750) {
            factors.add("No missed payments");
            factors.add("Good mix of credit types");
        }
        factors.add("Number of credit enquiries");
        return factors;
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return pan.substring(0, 3) + "****" + pan.substring(pan.length() - 1);
    }
}
