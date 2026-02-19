package com.loanflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Loan application creation request")
public class LoanApplicationRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer UUID", required = true)
    private UUID customerId;

    @NotBlank(message = "Loan type is required")
    @Schema(description = "Type of loan", example = "HOME_LOAN",
            allowableValues = {"HOME_LOAN", "PERSONAL_LOAN", "VEHICLE_LOAN", "BUSINESS_LOAN", "EDUCATION_LOAN"})
    private String loanType;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "10000", message = "Minimum loan amount is INR 10,000")
    @DecimalMax(value = "100000000", message = "Maximum loan amount is INR 10 Crore")
    @Schema(description = "Requested loan amount in INR", example = "5000000")
    private BigDecimal requestedAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 360, message = "Maximum tenure is 360 months (30 years)")
    @Schema(description = "Loan tenure in months", example = "240")
    private Integer tenureMonths;

    @NotBlank(message = "Purpose is required")
    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    @Schema(description = "Purpose of the loan", example = "Purchase of residential property")
    private String purpose;

    @Schema(description = "Branch code where application is submitted", example = "MUM001")
    private String branchCode;

    @Valid
    @Schema(description = "Property details for secured loans")
    private PropertyDetails propertyDetails;

    @Valid
    @Schema(description = "Employment details")
    private EmploymentDetails employmentDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Property collateral details")
    public static class PropertyDetails {

        @NotBlank(message = "Property type is required")
        @Schema(description = "Type of property", example = "APARTMENT")
        private String propertyType;

        @NotBlank(message = "Property address is required")
        @Schema(description = "Complete property address")
        private String address;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "State is required")
        private String state;

        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code")
        private String pinCode;

        @DecimalMin(value = "0", message = "Property value must be positive")
        @Schema(description = "Estimated property value in INR")
        private BigDecimal estimatedValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Applicant employment details")
    public static class EmploymentDetails {

        @NotBlank(message = "Employment type is required")
        @Schema(description = "Type of employment",
                allowableValues = {"SALARIED", "SELF_EMPLOYED", "BUSINESS", "PROFESSIONAL"})
        private String employmentType;

        @Schema(description = "Employer name for salaried")
        private String employerName;

        @Schema(description = "Business name for self-employed")
        private String businessName;

        @NotNull(message = "Monthly income is required")
        @DecimalMin(value = "0", message = "Income must be positive")
        @Schema(description = "Monthly income in INR")
        private BigDecimal monthlyIncome;

        @Min(value = 0, message = "Experience must be positive")
        @Schema(description = "Years of experience/business vintage")
        private Integer yearsOfExperience;
    }
}
