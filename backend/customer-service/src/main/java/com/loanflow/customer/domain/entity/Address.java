package com.loanflow.customer.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.StringJoiner;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "landmark", length = 100)
    private String landmark;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pin_code", length = 6)
    private String pinCode;

    @Column(name = "country", length = 2)
    @Builder.Default
    private String country = "IN";

    @Column(name = "ownership_type", length = 20)
    private String ownershipType; // OWNED, RENTED, FAMILY, COMPANY_PROVIDED

    @Column(name = "years_at_address")
    private Integer yearsAtAddress;

    /**
     * Get formatted full address
     */
    public String getFullAddress() {
        StringJoiner joiner = new StringJoiner(", ");

        if (addressLine1 != null && !addressLine1.isBlank()) {
            joiner.add(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.isBlank()) {
            joiner.add(addressLine2);
        }
        if (landmark != null && !landmark.isBlank()) {
            joiner.add(landmark);
        }
        if (city != null && !city.isBlank()) {
            joiner.add(city);
        }
        if (state != null && !state.isBlank()) {
            joiner.add(state);
        }
        if (pinCode != null && !pinCode.isBlank()) {
            joiner.add(pinCode);
        }
        if (country != null && !country.isBlank()) {
            joiner.add(country);
        }

        return joiner.toString();
    }

    /**
     * Validate PIN code format (Indian 6-digit)
     */
    public void validatePinCode() {
        if (pinCode == null || !pinCode.matches("^[1-9][0-9]{5}$")) {
            throw new IllegalArgumentException("Invalid PIN code format");
        }
    }
}
