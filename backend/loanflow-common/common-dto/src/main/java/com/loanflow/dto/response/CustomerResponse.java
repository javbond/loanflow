package com.loanflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Customer response")
public class CustomerResponse {

    @Schema(description = "Unique customer ID")
    private UUID id;

    @Schema(description = "Customer number", example = "CUS-2024-001234")
    private String customerNumber;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Middle name")
    private String middleName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Full name")
    private String fullName;

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @Schema(description = "Age in years")
    private Integer age;

    @Schema(description = "Gender")
    private String gender;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Mobile number")
    private String mobileNumber;

    @Schema(description = "PAN number (masked)")
    private String panNumber;

    @Schema(description = "Aadhaar number (masked)")
    private String aadhaarNumber;

    @Schema(description = "Aadhaar verified status")
    private Boolean aadhaarVerified;

    @Schema(description = "PAN verified status")
    private Boolean panVerified;

    @Schema(description = "KYC status")
    private String kycStatus;

    @Schema(description = "Customer segment")
    private String segment;

    @Schema(description = "Customer status")
    private String status;

    @Schema(description = "Current address")
    private AddressResponse currentAddress;

    @Schema(description = "Permanent address")
    private AddressResponse permanentAddress;

    @Schema(description = "Number of active loans")
    private Integer activeLoansCount;

    @Schema(description = "Total outstanding amount")
    private java.math.BigDecimal totalOutstanding;

    @Schema(description = "Customer since")
    private Instant createdAt;

    @Schema(description = "Last updated")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Address response")
    public static class AddressResponse {
        private String addressLine1;
        private String addressLine2;
        private String landmark;
        private String city;
        private String state;
        private String pinCode;
        private String country;
        private String ownershipType;
        private Integer yearsAtAddress;
        private String fullAddress;
    }
}
