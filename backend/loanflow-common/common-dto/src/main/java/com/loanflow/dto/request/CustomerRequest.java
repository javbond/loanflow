package com.loanflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer creation/update request")
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be 2-100 characters")
    @Schema(description = "Customer's first name", example = "Rahul")
    private String firstName;

    @Size(max = 100, message = "Middle name cannot exceed 100 characters")
    @Schema(description = "Customer's middle name")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be 2-100 characters")
    @Schema(description = "Customer's last name", example = "Sharma")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Schema(description = "Date of birth", example = "1990-05-15")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Schema(description = "Gender", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String gender;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "rahul.sharma@email.com")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    @Schema(description = "10-digit mobile number", example = "9876543210")
    private String mobileNumber;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    @Schema(description = "PAN card number", example = "ABCDE1234F")
    private String panNumber;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    @Schema(description = "Aadhaar number (12 digits)", example = "123456789012")
    private String aadhaarNumber;

    @Valid
    @NotNull(message = "Current address is required")
    @Schema(description = "Current residential address")
    private AddressRequest currentAddress;

    @Valid
    @Schema(description = "Permanent address (if different from current)")
    private AddressRequest permanentAddress;

    @Schema(description = "Customer segment", allowableValues = {"RETAIL", "HNI", "CORPORATE", "SME"})
    private String segment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Address details")
    public static class AddressRequest {

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 cannot exceed 200 characters")
        private String addressLine1;

        @Size(max = 200, message = "Address line 2 cannot exceed 200 characters")
        private String addressLine2;

        @Size(max = 100, message = "Landmark cannot exceed 100 characters")
        private String landmark;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        private String state;

        @NotBlank(message = "PIN code is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid PIN code")
        private String pinCode;

        @NotBlank(message = "Country is required")
        @Schema(description = "Country code", example = "IN")
        private String country;

        @Schema(description = "Address type", allowableValues = {"OWNED", "RENTED", "FAMILY", "COMPANY_PROVIDED"})
        private String ownershipType;

        @Min(value = 0, message = "Years at address must be positive")
        @Schema(description = "Years residing at this address")
        private Integer yearsAtAddress;
    }
}
