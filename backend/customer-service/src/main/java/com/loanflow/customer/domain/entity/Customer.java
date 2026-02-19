package com.loanflow.customer.domain.entity;

import com.loanflow.customer.domain.enums.CustomerStatus;
import com.loanflow.customer.domain.enums.Gender;
import com.loanflow.customer.domain.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Entity
@Table(name = "customers", schema = "identity",
        indexes = {
                @Index(name = "idx_customer_number", columnList = "customer_number", unique = true),
                @Index(name = "idx_customer_pan", columnList = "pan_number"),
                @Index(name = "idx_customer_aadhaar", columnList = "aadhaar_number"),
                @Index(name = "idx_customer_mobile", columnList = "mobile_number"),
                @Index(name = "idx_customer_email", columnList = "email"),
                @Index(name = "idx_customer_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private static final int MINOR_AGE = 18;
    private static final int SENIOR_CITIZEN_AGE = 60;
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_number", unique = true, length = 20)
    private String customerNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "aadhaar_verified")
    @Builder.Default
    private Boolean aadhaarVerified = false;

    @Column(name = "pan_verified")
    @Builder.Default
    private Boolean panVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "segment", length = 20)
    private String segment; // RETAIL, HNI, CORPORATE, SME

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine1", column = @Column(name = "current_address_line1")),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "current_address_line2")),
            @AttributeOverride(name = "landmark", column = @Column(name = "current_landmark")),
            @AttributeOverride(name = "city", column = @Column(name = "current_city")),
            @AttributeOverride(name = "state", column = @Column(name = "current_state")),
            @AttributeOverride(name = "pinCode", column = @Column(name = "current_pin_code")),
            @AttributeOverride(name = "country", column = @Column(name = "current_country")),
            @AttributeOverride(name = "ownershipType", column = @Column(name = "current_ownership_type")),
            @AttributeOverride(name = "yearsAtAddress", column = @Column(name = "current_years_at_address"))
    })
    private Address currentAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine1", column = @Column(name = "permanent_address_line1")),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "permanent_address_line2")),
            @AttributeOverride(name = "landmark", column = @Column(name = "permanent_landmark")),
            @AttributeOverride(name = "city", column = @Column(name = "permanent_city")),
            @AttributeOverride(name = "state", column = @Column(name = "permanent_state")),
            @AttributeOverride(name = "pinCode", column = @Column(name = "permanent_pin_code")),
            @AttributeOverride(name = "country", column = @Column(name = "permanent_country")),
            @AttributeOverride(name = "ownershipType", column = @Column(name = "permanent_ownership_type")),
            @AttributeOverride(name = "yearsAtAddress", column = @Column(name = "permanent_years_at_address"))
    })
    private Address permanentAddress;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    // ==================== Business Methods ====================

    /**
     * Generate unique customer number
     * Format: CUS-YYYY-NNNNNN
     */
    public void generateCustomerNumber() {
        if (this.customerNumber == null) {
            String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            long seq = counter.incrementAndGet() % 1000000;
            this.customerNumber = String.format("CUS-%s-%06d", year, seq);
        }
    }

    /**
     * Get full name
     */
    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            sb.append(" ").append(middleName);
        }
        sb.append(" ").append(lastName);
        return sb.toString();
    }

    /**
     * Calculate current age
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if customer is a minor (below 18)
     */
    public boolean isMinor() {
        return getAge() < MINOR_AGE;
    }

    /**
     * Check if customer is a senior citizen (60+)
     */
    public boolean isSeniorCitizen() {
        return getAge() >= SENIOR_CITIZEN_AGE;
    }

    /**
     * Verify Aadhaar
     */
    public void verifyAadhaar() {
        this.aadhaarVerified = true;
    }

    /**
     * Verify PAN
     */
    public void verifyPan() {
        this.panVerified = true;
    }

    /**
     * Update KYC status based on verifications
     */
    public void updateKycStatus() {
        if (Boolean.TRUE.equals(aadhaarVerified) && Boolean.TRUE.equals(panVerified)) {
            this.kycStatus = KycStatus.VERIFIED;
        } else if (Boolean.TRUE.equals(aadhaarVerified) || Boolean.TRUE.equals(panVerified)) {
            this.kycStatus = KycStatus.PARTIAL;
        } else {
            this.kycStatus = KycStatus.PENDING;
        }
    }

    /**
     * Check if KYC is complete
     */
    public boolean isKycComplete() {
        return kycStatus == KycStatus.VERIFIED;
    }

    /**
     * Check if customer is active
     */
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /**
     * Deactivate customer
     */
    public void deactivate(String reason) {
        this.status = CustomerStatus.INACTIVE;
    }

    /**
     * Block customer
     */
    public void block(String reason) {
        this.status = CustomerStatus.BLOCKED;
    }

    /**
     * Reactivate customer
     */
    public void reactivate() {
        if (this.status == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Cannot reactivate blocked customer");
        }
        this.status = CustomerStatus.ACTIVE;
    }

    // ==================== Validation Methods ====================

    /**
     * Validate PAN format
     */
    public void validatePan() {
        if (panNumber != null && !PAN_PATTERN.matcher(panNumber).matches()) {
            throw new IllegalArgumentException("Invalid PAN format");
        }
    }

    /**
     * Validate Aadhaar format
     */
    public void validateAadhaar() {
        if (aadhaarNumber != null && !AADHAAR_PATTERN.matcher(aadhaarNumber).matches()) {
            throw new IllegalArgumentException("Invalid Aadhaar format");
        }
    }

    /**
     * Validate mobile number format
     */
    public void validateMobile() {
        if (mobileNumber == null || !MOBILE_PATTERN.matcher(mobileNumber).matches()) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }
    }

    /**
     * Validate email format
     */
    public void validateEmail() {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    /**
     * Validate age (must be adult)
     */
    public void validateAge() {
        if (isMinor()) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
    }

    /**
     * Validate all fields
     */
    public void validate() {
        validatePan();
        validateAadhaar();
        validateMobile();
        validateEmail();
        validateAge();
    }

    // ==================== Masking Methods ====================

    /**
     * Get masked PAN (ABXX*****F)
     */
    public String getMaskedPan() {
        if (panNumber == null || panNumber.length() != 10) {
            return null;
        }
        return panNumber.substring(0, 2) + "XX*****" + panNumber.substring(9);
    }

    /**
     * Get masked Aadhaar (XXXX XXXX 9012)
     */
    public String getMaskedAadhaar() {
        if (aadhaarNumber == null || aadhaarNumber.length() != 12) {
            return null;
        }
        return "XXXX XXXX " + aadhaarNumber.substring(8);
    }
}
