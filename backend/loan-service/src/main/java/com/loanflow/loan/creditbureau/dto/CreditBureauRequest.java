package com.loanflow.loan.creditbureau.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for CIBIL credit bureau pull.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauRequest {

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String pan;

    private String firstName;
    private String lastName;
    private String dateOfBirth;   // dd-MM-yyyy
    private String gender;        // M/F
    private String mobileNumber;
    private double inquiryAmount;
}
