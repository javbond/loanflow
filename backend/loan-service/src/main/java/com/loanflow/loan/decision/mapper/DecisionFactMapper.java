package com.loanflow.loan.decision.mapper;

import com.loanflow.loan.decision.model.*;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps JPA LoanApplication entity to Drools fact POJOs.
 *
 * Since loan-service doesn't have direct access to customer-service data
 * (applicant details, employment, credit report), this mapper creates
 * stub facts with sensible defaults. Cross-service data integration
 * is planned for a future sprint.
 *
 * For the REST evaluation endpoint, facts can be provided directly
 * in the request body (ad-hoc evaluation).
 */
@Component
@Slf4j
public class DecisionFactMapper {

    /**
     * Map a JPA LoanApplication to Drools facts using defaults for
     * applicant/employment/credit data (since those are in customer-service).
     */
    public DecisionFacts mapToFacts(LoanApplication application) {
        String appId = application.getId().toString();
        String applicantId = UUID.randomUUID().toString();

        LoanApplicationFact appFact = LoanApplicationFact.builder()
                .id(appId)
                .applicationNumber(application.getApplicationNumber())
                .productCode(mapLoanTypeToProductCode(application.getLoanType()))
                .requestedAmount(application.getRequestedAmount().doubleValue())
                .tenureMonths(application.getTenureMonths())
                .propertyValue(0) // Default: no property value (set for HL via ad-hoc)
                .build();

        ApplicantFact applicantFact = ApplicantFact.builder()
                .id(applicantId)
                .applicationId(appId)
                .applicantType("PRIMARY")
                .age(35)               // Default age
                .gender("MALE")        // Default
                .pan("ABCDE1234F")     // Default valid PAN
                .panVerified(true)     // Default: verified
                .politicallyExposed(false)
                .existingEmi(0)
                .hasSalaryAccount(false)
                .existingCustomer(false)
                .existingLoanDpd(0)
                .build();

        EmploymentDetailsFact employmentFact = EmploymentDetailsFact.builder()
                .id(UUID.randomUUID().toString())
                .applicantId(applicantId)
                .employmentType(EmploymentType.SALARIED)
                .employerCategory(EmployerCategory.PRIVATE)
                .netMonthlyIncome(50000)     // Default: 50K/month
                .totalExperienceYears(5)
                .yearsInCurrentJob(2)
                .build();

        CreditReportFact creditFact = CreditReportFact.builder()
                .id(UUID.randomUUID().toString())
                .applicantId(applicantId)
                .creditScore(application.getCibilScore() != null ? application.getCibilScore() : 700)
                .dpd90PlusCount(0)
                .writtenOffAccounts(0)
                .enquiryCount30Days(1)
                .build();

        EligibilityResultFact eligibilityResult = EligibilityResultFact.builder()
                .applicationId(appId)
                .build();

        PricingResultFact pricingResult = PricingResultFact.builder()
                .applicationId(appId)
                .loanAmount(application.getRequestedAmount().doubleValue())
                .tenureMonths(application.getTenureMonths())
                .build();

        return new DecisionFacts(
                appFact,
                applicantFact,
                employmentFact,
                creditFact,
                eligibilityResult,
                pricingResult,
                null  // No collateral by default
        );
    }

    /**
     * Create facts from explicit parameters (for ad-hoc REST evaluation).
     */
    public DecisionFacts mapToFacts(
            LoanApplicationFact appFact,
            ApplicantFact applicantFact,
            EmploymentDetailsFact employmentFact,
            CreditReportFact creditFact,
            CollateralFact collateralFact
    ) {
        String appId = appFact.getId();

        EligibilityResultFact eligibilityResult = EligibilityResultFact.builder()
                .applicationId(appId)
                .build();

        PricingResultFact pricingResult = PricingResultFact.builder()
                .applicationId(appId)
                .loanAmount(appFact.getRequestedAmount())
                .tenureMonths(appFact.getTenureMonths())
                .build();

        return new DecisionFacts(
                appFact,
                applicantFact,
                employmentFact,
                creditFact,
                eligibilityResult,
                pricingResult,
                collateralFact
        );
    }

    /**
     * Map LoanType entity enum to DRL product code string.
     */
    public String mapLoanTypeToProductCode(LoanType loanType) {
        if (loanType == null) return "PL";
        return switch (loanType) {
            case PERSONAL_LOAN -> "PL";
            case HOME_LOAN -> "HL";
            case VEHICLE_LOAN -> "VL";
            case GOLD_LOAN -> "GL";
            case EDUCATION_LOAN -> "EL";
            case BUSINESS_LOAN -> "BL";
            case LAP -> "LAP";
        };
    }

    /**
     * Bundle record holding all Drools facts for a single evaluation.
     */
    public record DecisionFacts(
            LoanApplicationFact loanApplication,
            ApplicantFact applicant,
            EmploymentDetailsFact employmentDetails,
            CreditReportFact creditReport,
            EligibilityResultFact eligibilityResult,
            PricingResultFact pricingResult,
            CollateralFact collateral
    ) {}
}
