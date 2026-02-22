package com.loanflow.loan.decision.service;

import com.loanflow.loan.creditbureau.dto.CreditBureauResponse;
import com.loanflow.loan.decision.mapper.DecisionFactMapper;
import com.loanflow.loan.decision.mapper.DecisionFactMapper.DecisionFacts;
import com.loanflow.loan.decision.model.*;
import com.loanflow.loan.domain.entity.LoanApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Core Decision Engine Service — executes Drools rules against loan facts.
 *
 * Creates a new KieSession per evaluation (stateful, not shared).
 * KieContainer is thread-safe and shared across all evaluations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionEngineService {

    private final KieContainer kieContainer;
    private final ConfigService configService;
    private final RbiRateService rbiRateService;
    private final DecisionFactMapper factMapper;

    /**
     * Evaluate a loan application using Drools eligibility + pricing rules.
     *
     * @param application JPA LoanApplication entity
     * @return DecisionResult with eligibility status, interest rate, and pricing
     */
    public DecisionResult evaluate(LoanApplication application) {
        DecisionFacts facts = factMapper.mapToFacts(application);
        return evaluateWithFacts(facts, application.getApplicationNumber());
    }

    /**
     * Evaluate a loan application using real credit bureau data.
     *
     * @param application JPA LoanApplication entity
     * @param bureauResponse Real credit bureau response from CIBIL
     * @return DecisionResult with eligibility status, interest rate, and pricing
     */
    public DecisionResult evaluate(LoanApplication application, CreditBureauResponse bureauResponse) {
        DecisionFacts facts = factMapper.mapToFacts(application, bureauResponse);
        return evaluateWithFacts(facts, application.getApplicationNumber());
    }

    /**
     * Evaluate with explicit facts (for ad-hoc REST evaluation).
     */
    public DecisionResult evaluateWithFacts(DecisionFacts facts, String applicationNumber) {
        KieSession session = kieContainer.newKieSession();
        try {
            // Set globals required by DRL files
            session.setGlobal("logger", log);
            session.setGlobal("configService", configService);
            session.setGlobal("rbiRateService", rbiRateService);

            // Insert all facts into working memory
            session.insert(facts.loanApplication());
            session.insert(facts.applicant());
            session.insert(facts.employmentDetails());
            session.insert(facts.creditReport());
            session.insert(facts.eligibilityResult());
            session.insert(facts.pricingResult());
            if (facts.collateral() != null) {
                session.insert(facts.collateral());
            }

            // Fire all rules (max 200 to prevent infinite loops from rule re-triggering)
            int rulesFired = session.fireAllRules(200);
            log.info("Drools fired {} rules for application {}",
                    rulesFired, applicationNumber);

            // Build result from modified facts
            return buildResult(facts, rulesFired);

        } finally {
            session.dispose();
        }
    }

    private DecisionResult buildResult(DecisionFacts facts, int rulesFired) {
        EligibilityResultFact eligibility = facts.eligibilityResult();
        PricingResultFact pricing = facts.pricingResult();

        // Determine risk category from pricing tier or eligibility status
        String riskCategory = determineRiskCategory(pricing.getRiskTier(), eligibility.getStatus());

        // Determine overall decision
        String decision = determineDecision(eligibility.getStatus());

        return DecisionResult.builder()
                .eligible(eligibility.getStatus() == EligibilityStatus.ELIGIBLE
                        || eligibility.getStatus() == EligibilityStatus.CONDITIONALLY_ELIGIBLE)
                .eligibilityStatus(eligibility.getStatus() != null
                        ? eligibility.getStatus().name() : "UNKNOWN")
                .rejectionReasons(eligibility.getRejectionReasonList())
                .referReason(eligibility.getReferReason())
                .maxEligibleAmount(eligibility.getMaxEligibleAmount())
                .creditScore(facts.creditReport().getCreditScore())
                .riskCategory(riskCategory)
                .riskTier(pricing.getRiskTier())
                .interestRate(pricing.getFinalInterestRate())
                .baseRate(pricing.getBaseRate())
                .totalDiscounts(pricing.getTotalDiscounts())
                .totalPremiums(pricing.getTotalPremiums())
                .processingFee(pricing.getProcessingFee())
                .processingFeeWaiver(pricing.getProcessingFeeWaiver())
                .emi(pricing.getEmi())
                .decision(decision)
                .rulesFired(rulesFired)
                .build();
    }

    private String determineRiskCategory(String riskTier, EligibilityStatus status) {
        if (status == EligibilityStatus.REJECTED) return "HIGH";
        if (riskTier == null) return "MEDIUM";
        return switch (riskTier) {
            case "A" -> "LOW";
            case "B" -> "MEDIUM";
            case "C" -> "MEDIUM_HIGH";
            case "D" -> "HIGH";
            default -> "MEDIUM";
        };
    }

    private String determineDecision(EligibilityStatus status) {
        if (status == null) return "PENDING";
        return switch (status) {
            case ELIGIBLE, CONDITIONALLY_ELIGIBLE -> "APPROVED";
            case REJECTED, NOT_ELIGIBLE -> "REJECTED";
            case REFER -> "REFERRED";
            case PENDING_KYC -> "PENDING_KYC";
        };
    }

    /**
     * Decision result record — immutable result of Drools rule evaluation.
     */
    @lombok.Builder
    public record DecisionResult(
            boolean eligible,
            String eligibilityStatus,
            List<String> rejectionReasons,
            String referReason,
            double maxEligibleAmount,
            int creditScore,
            String riskCategory,
            String riskTier,
            double interestRate,
            double baseRate,
            double totalDiscounts,
            double totalPremiums,
            double processingFee,
            double processingFeeWaiver,
            double emi,
            String decision,
            int rulesFired
    ) {
        public DecisionResult {
            if (rejectionReasons == null) {
                rejectionReasons = new ArrayList<>();
            }
        }
    }
}
