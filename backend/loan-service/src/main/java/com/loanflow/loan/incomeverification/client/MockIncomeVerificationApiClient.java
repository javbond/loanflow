package com.loanflow.loan.incomeverification.client;

import com.loanflow.loan.incomeverification.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock income verification API client for dev/uat/test environments.
 * Generates deterministic income data based on PAN hash:
 * - Same PAN always returns the same verified income (reproducible testing)
 * - ITR data always present
 * - GST data only generated when GSTIN is provided
 * - Bank statement data with 6 months of balances
 * - Income consistency score derived from declared vs verified income
 */
@Component
@Profile({"dev", "uat", "test", "default"})
@Slf4j
public class MockIncomeVerificationApiClient implements IncomeVerificationApiClient {

    @Override
    public IncomeVerificationResponse verifyIncome(IncomeVerificationRequest request) {
        log.info("Mock Income Verification: Verifying income for PAN {}", maskPan(request.getPan()));

        int hash = Math.abs(request.getPan().hashCode());

        // Generate verified monthly income (deterministic from PAN)
        BigDecimal verifiedMonthlyIncome = generateVerifiedIncome(hash);

        // Calculate income consistency vs declared
        int consistencyScore = calculateConsistencyScore(
                request.getDeclaredMonthlyIncome(), verifiedMonthlyIncome);

        // Generate ITR data (always present)
        ItrData itrData = generateItrData(hash, request.getEmploymentType(), verifiedMonthlyIncome);

        // Generate GST data (only if GSTIN provided)
        GstData gstData = request.getGstin() != null && !request.getGstin().isBlank()
                ? generateGstData(hash, request.getGstin())
                : null;

        // Generate bank statement data (always present)
        BankStatementData bankData = generateBankStatementData(hash, verifiedMonthlyIncome);

        // Calculate DTI ratio (using mock total EMI obligations)
        BigDecimal totalEmi = verifiedMonthlyIncome.multiply(BigDecimal.valueOf(0.1 + (hash % 40) / 100.0))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal dtiRatio = verifiedMonthlyIncome.compareTo(BigDecimal.ZERO) > 0
                ? totalEmi.divide(verifiedMonthlyIncome, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Generate warning flags
        List<String> flags = generateFlags(consistencyScore, bankData.getBounceCount(), dtiRatio);

        IncomeVerificationResponse response = IncomeVerificationResponse.builder()
                .pan(request.getPan())
                .incomeVerified(true)
                .verifiedMonthlyIncome(verifiedMonthlyIncome)
                .dtiRatio(dtiRatio)
                .incomeConsistencyScore(consistencyScore)
                .itrData(itrData)
                .gstData(gstData)
                .bankStatementData(bankData)
                .flags(flags)
                .dataSource(IncomeDataSource.REAL) // Simulates the "real" client path
                .verificationTimestamp(Instant.now())
                .build();

        log.info("Mock Income Verification: PAN {} → Verified Income=₹{}, DTI={}, Consistency={}%",
                maskPan(request.getPan()), verifiedMonthlyIncome, dtiRatio, consistencyScore);

        return response;
    }

    /**
     * Deterministic verified monthly income from PAN hash.
     * Range: ₹25,000 - ₹5,00,000
     */
    BigDecimal generateVerifiedIncome(int hash) {
        long baseIncome = 25000L + (hash % 475001); // 25k - 5L
        return BigDecimal.valueOf(baseIncome).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate income consistency score (0-100).
     * Measures how closely declared income matches verified income.
     */
    int calculateConsistencyScore(BigDecimal declared, BigDecimal verified) {
        if (declared == null || declared.compareTo(BigDecimal.ZERO) <= 0) {
            return 50; // Unknown declared income — neutral score
        }
        double ratio = declared.doubleValue() / verified.doubleValue();
        if (ratio > 1.0) ratio = 1.0 / ratio; // Symmetric — over or under-declaration penalized equally
        return Math.min(100, Math.max(0, (int) (ratio * 100)));
    }

    private ItrData generateItrData(int hash, String employmentType, BigDecimal verifiedMonthlyIncome) {
        BigDecimal annualIncome = verifiedMonthlyIncome.multiply(BigDecimal.valueOf(12));
        boolean isSalaried = "SALARIED".equals(employmentType);

        String itrForm = isSalaried ? "ITR-1" : "ITR-3";
        BigDecimal salaryIncome = isSalaried ? annualIncome : BigDecimal.ZERO;
        BigDecimal businessIncome = isSalaried ? BigDecimal.ZERO : annualIncome;

        return ItrData.builder()
                .grossTotalIncome(annualIncome)
                .salaryIncome(salaryIncome)
                .businessIncome(businessIncome)
                .itrFormType(itrForm)
                .assessmentYear("2025-26")
                .filedOnTime(hash % 5 != 0) // 80% filed on time
                .build();
    }

    private GstData generateGstData(int hash, String gstin) {
        BigDecimal turnover = BigDecimal.valueOf(500000L + (hash % 5000000)); // 5L - 55L
        int filings = 8 + (hash % 5); // 8-12 filings in last 12 months
        String[] ratings = {"EXCELLENT", "GOOD", "FAIR", "POOR"};
        String rating = ratings[hash % 4];

        return GstData.builder()
                .gstin(gstin)
                .annualTurnover(turnover)
                .complianceRating(rating)
                .filingCount(filings)
                .active(true)
                .build();
    }

    BankStatementData generateBankStatementData(int hash, BigDecimal monthlyIncome) {
        List<BigDecimal> monthlyBalances = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal min = BigDecimal.valueOf(Long.MAX_VALUE);
        BigDecimal max = BigDecimal.ZERO;

        for (int i = 0; i < 6; i++) {
            // Generate balance as 0.5x to 3x of monthly income
            double factor = 0.5 + ((hash + i * 7) % 250) / 100.0;
            BigDecimal balance = monthlyIncome.multiply(BigDecimal.valueOf(factor))
                    .setScale(2, RoundingMode.HALF_UP);
            monthlyBalances.add(balance);
            total = total.add(balance);
            if (balance.compareTo(min) < 0) min = balance;
            if (balance.compareTo(max) > 0) max = balance;
        }

        BigDecimal avgBalance = total.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        int bounceCount = hash % 8; // 0-7 bounces

        return BankStatementData.builder()
                .avgMonthlyBalance(avgBalance)
                .avgMonthlyCredits(monthlyIncome)
                .bounceCount(bounceCount)
                .monthlyBalances(monthlyBalances)
                .monthsAnalyzed(6)
                .minBalance(min)
                .maxBalance(max)
                .build();
    }

    private List<String> generateFlags(int consistencyScore, int bounceCount, BigDecimal dtiRatio) {
        List<String> flags = new ArrayList<>();

        if (consistencyScore < 70) {
            flags.add("INCOME_MISMATCH: Declared income differs significantly from verified income");
        }
        if (bounceCount > 3) {
            flags.add("HIGH_BOUNCE_COUNT: " + bounceCount + " cheque/ECS bounces in 6 months");
        }
        if (dtiRatio.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            flags.add("HIGH_DTI: Debt-to-Income ratio exceeds 50%");
        }
        if (dtiRatio.compareTo(BigDecimal.valueOf(0.4)) > 0 && dtiRatio.compareTo(BigDecimal.valueOf(0.5)) <= 0) {
            flags.add("ELEVATED_DTI: Debt-to-Income ratio between 40-50%");
        }

        return flags;
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return pan.substring(0, 3) + "****" + pan.substring(pan.length() - 1);
    }
}
