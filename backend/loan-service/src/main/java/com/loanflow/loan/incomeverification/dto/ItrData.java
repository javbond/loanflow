package com.loanflow.loan.incomeverification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ITR (Income Tax Return) verification data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItrData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Gross total income from latest ITR */
    private BigDecimal grossTotalIncome;

    /** Salary income component */
    private BigDecimal salaryIncome;

    /** Business/profession income component */
    private BigDecimal businessIncome;

    /** ITR form type: ITR-1, ITR-2, ITR-3, ITR-4 */
    private String itrFormType;

    /** Assessment year, e.g., "2025-26" */
    private String assessmentYear;

    /** Whether ITR was filed on time */
    private boolean filedOnTime;
}
