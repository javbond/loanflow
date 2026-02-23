package com.loanflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Demographic data from UIDAI e-KYC verification")
public class EkycData {

    @Schema(description = "Full name as per Aadhaar", example = "Rahul Sharma")
    private String name;

    @Schema(description = "Date of birth (dd-MM-yyyy)", example = "15-05-1990")
    private String dateOfBirth;

    @Schema(description = "Gender", example = "MALE")
    private String gender;

    @Schema(description = "Full address as per Aadhaar")
    private String address;

    @Schema(description = "Base64-encoded photo from Aadhaar")
    private String photo;

    @Schema(description = "PIN code from address", example = "400001")
    private String pinCode;

    @Schema(description = "State from address", example = "Maharashtra")
    private String state;

    @Schema(description = "District from address", example = "Mumbai")
    private String district;
}
