package com.loanflow.customer.domain;

import com.loanflow.customer.domain.entity.Address;
import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.enums.CustomerStatus;
import com.loanflow.customer.domain.enums.Gender;
import com.loanflow.customer.domain.enums.KycStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Cases for Customer Entity
 * Tests written FIRST, then implementation follows
 */
@DisplayName("Customer Entity Tests")
class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .firstName("Rahul")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .email("rahul.sharma@email.com")
                .mobileNumber("9876543210")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .build();
    }

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create customer with ACTIVE status")
        void shouldCreateWithActiveStatus() {
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should generate customer number on creation")
        void shouldGenerateCustomerNumber() {
            customer.generateCustomerNumber();
            assertThat(customer.getCustomerNumber())
                    .isNotNull()
                    .startsWith("CUS-");
        }

        @Test
        @DisplayName("Should have PENDING KYC status initially")
        void shouldHavePendingKycStatus() {
            assertThat(customer.getKycStatus()).isEqualTo(KycStatus.PENDING);
        }

        @Test
        @DisplayName("Should calculate full name correctly")
        void shouldCalculateFullName() {
            assertThat(customer.getFullName()).isEqualTo("Rahul Sharma");
        }

        @Test
        @DisplayName("Should calculate full name with middle name")
        void shouldCalculateFullNameWithMiddleName() {
            customer.setMiddleName("Kumar");
            assertThat(customer.getFullName()).isEqualTo("Rahul Kumar Sharma");
        }
    }

    @Nested
    @DisplayName("Age Calculation Tests")
    class AgeTests {

        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAge() {
            // Born on 1990-05-15, assuming current date is 2026-02-17
            int age = customer.getAge();
            assertThat(age).isBetween(35, 36); // Depends on exact current date
        }

        @Test
        @DisplayName("Should check if customer is minor")
        void shouldCheckIfMinor() {
            customer.setDateOfBirth(LocalDate.now().minusYears(17));
            assertThat(customer.isMinor()).isTrue();

            customer.setDateOfBirth(LocalDate.now().minusYears(18));
            assertThat(customer.isMinor()).isFalse();
        }

        @Test
        @DisplayName("Should check if customer is senior citizen")
        void shouldCheckIfSeniorCitizen() {
            customer.setDateOfBirth(LocalDate.now().minusYears(59));
            assertThat(customer.isSeniorCitizen()).isFalse();

            customer.setDateOfBirth(LocalDate.now().minusYears(60));
            assertThat(customer.isSeniorCitizen()).isTrue();
        }
    }

    @Nested
    @DisplayName("KYC Verification Tests")
    class KycTests {

        @Test
        @DisplayName("Should verify Aadhaar")
        void shouldVerifyAadhaar() {
            customer.verifyAadhaar();
            assertThat(customer.getAadhaarVerified()).isTrue();
        }

        @Test
        @DisplayName("Should verify PAN")
        void shouldVerifyPan() {
            customer.verifyPan();
            assertThat(customer.getPanVerified()).isTrue();
        }

        @Test
        @DisplayName("Should update KYC status to VERIFIED when both verified")
        void shouldUpdateKycStatusWhenBothVerified() {
            customer.verifyAadhaar();
            customer.verifyPan();
            customer.updateKycStatus();

            assertThat(customer.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
        }

        @Test
        @DisplayName("Should have PARTIAL KYC status when only one verified")
        void shouldHavePartialKycWhenOneVerified() {
            customer.verifyAadhaar();
            customer.updateKycStatus();

            assertThat(customer.getKycStatus()).isEqualTo(KycStatus.PARTIAL);
        }

        @Test
        @DisplayName("Should check if KYC is complete")
        void shouldCheckIfKycComplete() {
            assertThat(customer.isKycComplete()).isFalse();

            customer.verifyAadhaar();
            customer.verifyPan();
            customer.updateKycStatus();

            assertThat(customer.isKycComplete()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate PAN format")
        void shouldValidatePanFormat() {
            // Valid PAN
            assertThatCode(() -> customer.validatePan())
                    .doesNotThrowAnyException();

            // Invalid PAN
            customer.setPanNumber("INVALID");
            assertThatThrownBy(() -> customer.validatePan())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid PAN format");
        }

        @Test
        @DisplayName("Should validate Aadhaar format")
        void shouldValidateAadhaarFormat() {
            // Valid Aadhaar (12 digits)
            assertThatCode(() -> customer.validateAadhaar())
                    .doesNotThrowAnyException();

            // Invalid Aadhaar
            customer.setAadhaarNumber("12345");
            assertThatThrownBy(() -> customer.validateAadhaar())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Aadhaar format");
        }

        @Test
        @DisplayName("Should validate mobile number format")
        void shouldValidateMobileFormat() {
            // Valid mobile (10 digits starting with 6-9)
            assertThatCode(() -> customer.validateMobile())
                    .doesNotThrowAnyException();

            // Invalid mobile
            customer.setMobileNumber("1234567890"); // Starts with 1
            assertThatThrownBy(() -> customer.validateMobile())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid mobile number");
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            // Valid email
            assertThatCode(() -> customer.validateEmail())
                    .doesNotThrowAnyException();

            // Invalid email
            customer.setEmail("invalid-email");
            assertThatThrownBy(() -> customer.validateEmail())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Should validate date of birth")
        void shouldValidateDateOfBirth() {
            // Valid DOB (adult)
            assertThatCode(() -> customer.validateAge())
                    .doesNotThrowAnyException();

            // Minor
            customer.setDateOfBirth(LocalDate.now().minusYears(15));
            assertThatThrownBy(() -> customer.validateAge())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be at least 18 years");
        }
    }

    @Nested
    @DisplayName("Address Tests")
    class AddressTests {

        @Test
        @DisplayName("Should set current address")
        void shouldSetCurrentAddress() {
            Address address = Address.builder()
                    .addressLine1("123 Main Street")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pinCode("400001")
                    .country("IN")
                    .build();

            customer.setCurrentAddress(address);

            assertThat(customer.getCurrentAddress()).isNotNull();
            assertThat(customer.getCurrentAddress().getCity()).isEqualTo("Mumbai");
        }

        @Test
        @DisplayName("Should get full address string")
        void shouldGetFullAddress() {
            Address address = Address.builder()
                    .addressLine1("123 Main Street")
                    .addressLine2("Near Park")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pinCode("400001")
                    .country("IN")
                    .build();

            customer.setCurrentAddress(address);

            String fullAddress = customer.getCurrentAddress().getFullAddress();
            assertThat(fullAddress).contains("123 Main Street", "Mumbai", "400001");
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusTests {

        @Test
        @DisplayName("Should deactivate customer")
        void shouldDeactivateCustomer() {
            customer.deactivate("Customer request");

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        }

        @Test
        @DisplayName("Should block customer")
        void shouldBlockCustomer() {
            customer.block("Fraud detected");

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
        }

        @Test
        @DisplayName("Should reactivate customer")
        void shouldReactivateCustomer() {
            customer.deactivate("Temporary");
            customer.reactivate();

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should not reactivate blocked customer")
        void shouldNotReactivateBlocked() {
            customer.block("Fraud");

            assertThatThrownBy(() -> customer.reactivate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot reactivate blocked customer");
        }

        @Test
        @DisplayName("Should check if customer is active")
        void shouldCheckIfActive() {
            assertThat(customer.isActive()).isTrue();

            customer.deactivate("Test");
            assertThat(customer.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("PAN Masking Tests")
    class MaskingTests {

        @Test
        @DisplayName("Should mask PAN number")
        void shouldMaskPan() {
            // PAN: ABCDE1234F -> ABXX*****F
            String masked = customer.getMaskedPan();
            assertThat(masked).isEqualTo("ABXX*****F");
        }

        @Test
        @DisplayName("Should mask Aadhaar number")
        void shouldMaskAadhaar() {
            // Aadhaar: 123456789012 -> XXXX XXXX 9012
            String masked = customer.getMaskedAadhaar();
            assertThat(masked).isEqualTo("XXXX XXXX 9012");
        }
    }
}
