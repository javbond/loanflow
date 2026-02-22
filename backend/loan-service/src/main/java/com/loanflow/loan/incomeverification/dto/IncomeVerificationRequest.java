package com.loanflow.loan.incomeverification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for income verification.
 * PAN is mandatory; GSTIN is optional (only for self-employed/business).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeVerificationRequest {

    @NotBlank(message = "PAN is required for income verification")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String pan;

    /** GSTIN — optional, used for GST verification (self-employed/business) */
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "Invalid GSTIN format")
    private String gstin;

    /** Employment type: SALARIED, SELF_EMPLOYED, BUSINESS, PROFESSIONAL */
    private String employmentType;

    /** Declared monthly income from loan application */
    private BigDecimal declaredMonthlyIncome;
}
