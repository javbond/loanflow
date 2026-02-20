package com.loanflow.loan.dto;

import com.loanflow.loan.domain.enums.LoanType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for customer loan application submission
 * Issue: #26 [US-024] Customer Loan Application Form
 */
public record CustomerLoanApplicationRequest(
    // Loan Details
    @NotNull(message = "Loan type is required")
    LoanType loanType,

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "10000", message = "Minimum loan amount is ₹10,000")
    @DecimalMax(value = "50000000", message = "Maximum loan amount is ₹5 Crore")
    BigDecimal requestedAmount,

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    Integer tenureMonths,

    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    String purpose,

    // Personal Details
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    String fullName,

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    String pan,

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    String aadhaar,

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid Indian mobile number")
    String phone,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    String address,

    // Employment Details
    @NotBlank(message = "Employment type is required")
    String employmentType, // SALARIED, SELF_EMPLOYED, BUSINESS

    @Size(max = 100, message = "Employer name cannot exceed 100 characters")
    String employerName,

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "10000", message = "Minimum monthly income is ₹10,000")
    BigDecimal monthlyIncome
) {}
