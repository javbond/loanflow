package com.loanflow.loan.decision.controller;

import com.loanflow.loan.decision.mapper.DecisionFactMapper;
import com.loanflow.loan.decision.mapper.DecisionFactMapper.DecisionFacts;
import com.loanflow.loan.decision.model.*;
import com.loanflow.loan.decision.service.DecisionEngineService;
import com.loanflow.loan.decision.service.DecisionEngineService.DecisionResult;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.repository.LoanApplicationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for the Drools Decision Engine.
 * Allows staff to manually trigger or preview rule evaluation
 * without going through the full BPMN workflow.
 */
@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
@Slf4j
public class DecisionController {

    private final DecisionEngineService decisionEngineService;
    private final DecisionFactMapper factMapper;
    private final LoanApplicationRepository loanApplicationRepository;

    /**
     * Evaluate a loan application using Drools rules.
     * Can evaluate by applicationId (existing loan) or ad-hoc with inline facts.
     */
    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SENIOR_UNDERWRITER', 'BRANCH_MANAGER')")
    public ResponseEntity<DecisionResult> evaluate(@Valid @RequestBody EvaluationRequest request) {
        log.info("Decision evaluation requested: {}", request);

        DecisionResult result;

        if (request.applicationId() != null) {
            // Evaluate existing application
            LoanApplication application = loanApplicationRepository.findById(request.applicationId())
                    .orElseThrow(() -> new RuntimeException(
                            "Application not found: " + request.applicationId()));

            result = decisionEngineService.evaluate(application);
        } else {
            // Ad-hoc evaluation with inline facts
            DecisionFacts facts = buildFactsFromRequest(request);
            result = decisionEngineService.evaluateWithFacts(facts,
                    request.applicationNumber() != null ? request.applicationNumber() : "AD-HOC");
        }

        return ResponseEntity.ok(result);
    }

    private DecisionFacts buildFactsFromRequest(EvaluationRequest request) {
        String appId = UUID.randomUUID().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId)
                .applicationNumber(request.applicationNumber() != null ? request.applicationNumber() : "AD-HOC")
                .productCode(request.productCode() != null ? request.productCode() : "PL")
                .requestedAmount(request.requestedAmount())
                .tenureMonths(request.tenureMonths() > 0 ? request.tenureMonths() : 60)
                .propertyValue(request.propertyValue())
                .build();

        ApplicantFact applicantFact = ApplicantFact.builder()
                .id(applicantId)
                .applicationId(appId)
                .applicantType("PRIMARY")
                .age(request.applicantAge() > 0 ? request.applicantAge() : 35)
                .gender(request.applicantGender() != null ? request.applicantGender() : "MALE")
                .pan(request.pan() != null ? request.pan() : "ABCDE1234F")
                .panVerified(true)
                .politicallyExposed(false)
                .existingEmi(request.existingEmi())
                .hasSalaryAccount(request.hasSalaryAccount())
                .existingCustomer(false)
                .existingLoanDpd(0)
                .build();

        EmploymentDetailsFact empFact = EmploymentDetailsFact.builder()
                .id(UUID.randomUUID().toString())
                .applicantId(applicantId)
                .employmentType(request.employmentType() != null
                        ? EmploymentType.valueOf(request.employmentType()) : EmploymentType.SALARIED)
                .employerCategory(request.employerCategory() != null
                        ? EmployerCategory.valueOf(request.employerCategory()) : EmployerCategory.PRIVATE)
                .netMonthlyIncome(request.netMonthlyIncome() > 0 ? request.netMonthlyIncome() : 50000)
                .totalExperienceYears(request.totalExperienceYears() > 0 ? request.totalExperienceYears() : 5)
                .yearsInCurrentJob(request.yearsInCurrentJob() > 0 ? request.yearsInCurrentJob() : 2)
                .build();

        CreditReportFact creditFact = CreditReportFact.builder()
                .id(UUID.randomUUID().toString())
                .applicantId(applicantId)
                .creditScore(request.creditScore() > 0 ? request.creditScore() : 700)
                .dpd90PlusCount(request.dpd90PlusCount())
                .writtenOffAccounts(request.writtenOffAccounts())
                .enquiryCount30Days(request.enquiryCount30Days())
                .build();

        CollateralFact collateralFact = null;
        if (request.collateralValue() > 0) {
            collateralFact = CollateralFact.builder()
                    .id(UUID.randomUUID().toString())
                    .applicationId(appId)
                    .marketValue(request.collateralValue())
                    .build();
        }

        return factMapper.mapToFacts(appFact, applicantFact, empFact, creditFact, collateralFact);
    }

    /**
     * Request body for decision evaluation.
     * Either provide applicationId (for existing loan) or inline parameters (ad-hoc).
     */
    public record EvaluationRequest(
            UUID applicationId,
            String applicationNumber,
            String productCode,
            double requestedAmount,
            int tenureMonths,
            double propertyValue,
            int applicantAge,
            String applicantGender,
            String pan,
            double existingEmi,
            boolean hasSalaryAccount,
            String employmentType,
            String employerCategory,
            double netMonthlyIncome,
            double totalExperienceYears,
            double yearsInCurrentJob,
            int creditScore,
            int dpd90PlusCount,
            int writtenOffAccounts,
            int enquiryCount30Days,
            double collateralValue
    ) {}
}
