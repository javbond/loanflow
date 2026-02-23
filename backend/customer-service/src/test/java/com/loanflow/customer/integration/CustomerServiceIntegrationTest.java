package com.loanflow.customer.integration;

import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.customer.service.CustomerService;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import com.loanflow.util.exception.DuplicateResourceException;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for CustomerService with real PostgreSQL via Testcontainers.
 * Tests CRUD operations, duplicate detection, KYC verification, and status management
 * against an actual database with Flyway migrations applied.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CustomerService customerService;

    private static String createdCustomerId;
    private static String createdCustomerNumber;

    // ==================== Test Data Builders ====================

    private CustomerRequest.AddressRequest buildAddress() {
        return CustomerRequest.AddressRequest.builder()
                .addressLine1("123 MG Road")
                .addressLine2("Near City Mall")
                .landmark("Opposite Metro Station")
                .city("Bangalore")
                .state("Karnataka")
                .pinCode("560001")
                .country("IN")
                .ownershipType("OWNED")
                .yearsAtAddress(5)
                .build();
    }

    private CustomerRequest buildCustomerRequest(String firstName, String lastName,
                                                  String email, String mobile) {
        return CustomerRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE")
                .email(email)
                .mobileNumber(mobile)
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .currentAddress(buildAddress())
                .segment("RETAIL")
                .build();
    }

    // ==================== Create Tests ====================

    @Nested
    @DisplayName("Customer Creation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateTests {

        @Test
        @Order(1)
        @DisplayName("Should create customer with valid data and auto-generate customer number")
        void shouldCreateCustomer() {
            CustomerRequest request = buildCustomerRequest(
                    "Rahul", "Sharma", "rahul.sharma@test.com", "9876543210");

            CustomerResponse response = customerService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getCustomerNumber()).startsWith("CUS-");
            assertThat(response.getFirstName()).isEqualTo("Rahul");
            assertThat(response.getLastName()).isEqualTo("Sharma");
            assertThat(response.getEmail()).isEqualTo("rahul.sharma@test.com");
            assertThat(response.getKycStatus()).isEqualTo("PENDING");
            assertThat(response.getStatus()).isEqualTo("ACTIVE");

            // Store for subsequent tests
            createdCustomerId = response.getId().toString();
            createdCustomerNumber = response.getCustomerNumber();
        }

        @Test
        @Order(2)
        @DisplayName("Should reject duplicate PAN number")
        void shouldRejectDuplicatePan() {
            CustomerRequest request = buildCustomerRequest(
                    "Priya", "Patel", "priya.patel@test.com", "9876543211");
            request.setPanNumber("ABCDE1234F"); // Same PAN as first customer

            assertThatThrownBy(() -> customerService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("PAN number");
        }

        @Test
        @Order(3)
        @DisplayName("Should reject duplicate email")
        void shouldRejectDuplicateEmail() {
            CustomerRequest request = buildCustomerRequest(
                    "Amit", "Kumar", "rahul.sharma@test.com", "9876543212");
            request.setPanNumber("FGHIJ5678K");
            request.setAadhaarNumber("234567890123");

            assertThatThrownBy(() -> customerService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @Order(4)
        @DisplayName("Should reject duplicate mobile number")
        void shouldRejectDuplicateMobile() {
            CustomerRequest request = buildCustomerRequest(
                    "Neha", "Gupta", "neha.gupta@test.com", "9876543210");
            request.setPanNumber("KLMNO9012P");
            request.setAadhaarNumber("345678901234");

            assertThatThrownBy(() -> customerService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("mobile number");
        }

        @Test
        @Order(5)
        @DisplayName("Should create second customer with unique details")
        void shouldCreateSecondCustomer() {
            CustomerRequest request = buildCustomerRequest(
                    "Priya", "Patel", "priya.patel@test.com", "9876543211");
            request.setPanNumber("FGHIJ5678K");
            request.setAadhaarNumber("234567890123");

            CustomerResponse response = customerService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getFirstName()).isEqualTo("Priya");
            assertThat(response.getCustomerNumber()).startsWith("CUS-");
            assertThat(response.getCustomerNumber()).isNotEqualTo(createdCustomerNumber);
        }
    }

    // ==================== Read Tests ====================

    @Nested
    @DisplayName("Customer Retrieval")
    class ReadTests {

        @Test
        @DisplayName("Should retrieve customer by ID")
        void shouldGetById() {
            // Create a customer first
            CustomerRequest request = buildCustomerRequest(
                    "Suresh", "Reddy", "suresh.reddy@test.com", "8765432109");
            request.setPanNumber("PQRST3456U");
            request.setAadhaarNumber("456789012345");
            CustomerResponse created = customerService.create(request);

            CustomerResponse retrieved = customerService.getById(created.getId());

            assertThat(retrieved.getId()).isEqualTo(created.getId());
            assertThat(retrieved.getFirstName()).isEqualTo("Suresh");
            assertThat(retrieved.getEmail()).isEqualTo("suresh.reddy@test.com");
        }

        @Test
        @DisplayName("Should retrieve customer by customer number")
        void shouldGetByCustomerNumber() {
            CustomerRequest request = buildCustomerRequest(
                    "Meera", "Iyer", "meera.iyer@test.com", "7654321098");
            request.setPanNumber("UVWXY6789Z");
            request.setAadhaarNumber("567890123456");
            CustomerResponse created = customerService.create(request);

            CustomerResponse retrieved = customerService.getByCustomerNumber(created.getCustomerNumber());

            assertThat(retrieved.getCustomerNumber()).isEqualTo(created.getCustomerNumber());
            assertThat(retrieved.getFirstName()).isEqualTo("Meera");
        }

        @Test
        @DisplayName("Should retrieve customer by PAN number")
        void shouldGetByPan() {
            CustomerRequest request = buildCustomerRequest(
                    "Vikram", "Singh", "vikram.singh@test.com", "6543210987");
            request.setPanNumber("GHIJK7890L");
            request.setAadhaarNumber("678901234567");
            CustomerResponse created = customerService.create(request);

            CustomerResponse retrieved = customerService.getByPan("GHIJK7890L");

            assertThat(retrieved.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent ID")
        void shouldThrowForNonExistentId() {
            assertThatThrownBy(() -> customerService.getById(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should paginate all customers")
        void shouldPaginateAll() {
            Page<CustomerResponse> page = customerService.getAll(PageRequest.of(0, 10));

            assertThat(page).isNotNull();
            assertThat(page.getContent()).isNotEmpty();
            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        }
    }

    // ==================== Update Tests ====================

    @Nested
    @DisplayName("Customer Update")
    class UpdateTests {

        @Test
        @DisplayName("Should update customer details")
        void shouldUpdateCustomer() {
            // Create
            CustomerRequest createRequest = buildCustomerRequest(
                    "Ankit", "Verma", "ankit.verma@test.com", "9123456780");
            createRequest.setPanNumber("LMNOP1234Q");
            createRequest.setAadhaarNumber("789012345678");
            CustomerResponse created = customerService.create(createRequest);

            // Update
            CustomerRequest updateRequest = buildCustomerRequest(
                    "Ankit", "Verma-Updated", "ankit.verma@test.com", "9123456780");
            updateRequest.setPanNumber("LMNOP1234Q");
            updateRequest.setAadhaarNumber("789012345678");

            CustomerResponse updated = customerService.update(created.getId(), updateRequest);

            assertThat(updated.getLastName()).isEqualTo("Verma-Updated");
            assertThat(updated.getId()).isEqualTo(created.getId());
        }
    }

    // ==================== KYC Tests ====================

    @Nested
    @DisplayName("KYC Verification")
    class KycTests {

        @Test
        @DisplayName("Should verify Aadhaar and update KYC status to PARTIAL")
        void shouldVerifyAadhaarToPartial() {
            CustomerRequest request = buildCustomerRequest(
                    "Deepak", "Nair", "deepak.nair@test.com", "8012345678");
            request.setPanNumber("RSTUV5678W");
            request.setAadhaarNumber("890123456789");
            CustomerResponse created = customerService.create(request);

            CustomerResponse afterAadhaar = customerService.verifyAadhaar(created.getId());

            assertThat(afterAadhaar.getKycStatus()).isEqualTo("PARTIAL");
        }

        @Test
        @DisplayName("Should verify both Aadhaar and PAN to reach VERIFIED KYC status")
        void shouldReachVerifiedKycStatus() {
            CustomerRequest request = buildCustomerRequest(
                    "Lakshmi", "Menon", "lakshmi.menon@test.com", "7012345678");
            request.setPanNumber("XYZAB1234C");
            request.setAadhaarNumber("901234567890");
            CustomerResponse created = customerService.create(request);

            // Verify Aadhaar first
            customerService.verifyAadhaar(created.getId());

            // Verify PAN
            CustomerResponse afterBoth = customerService.verifyPan(created.getId());

            assertThat(afterBoth.getKycStatus()).isEqualTo("VERIFIED");
        }
    }

    // ==================== Status Management Tests ====================

    @Nested
    @DisplayName("Status Management")
    class StatusTests {

        @Test
        @DisplayName("Should deactivate and reactivate customer")
        void shouldDeactivateAndReactivate() {
            CustomerRequest request = buildCustomerRequest(
                    "Rajesh", "Pillai", "rajesh.pillai@test.com", "6012345678");
            request.setPanNumber("DEFGH9012I");
            request.setAadhaarNumber("012345678901");
            CustomerResponse created = customerService.create(request);

            // Deactivate
            CustomerResponse deactivated = customerService.deactivate(
                    created.getId(), "Customer request");
            assertThat(deactivated.getStatus()).isEqualTo("INACTIVE");

            // Reactivate
            CustomerResponse reactivated = customerService.reactivate(created.getId());
            assertThat(reactivated.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should block customer")
        void shouldBlockCustomer() {
            CustomerRequest request = buildCustomerRequest(
                    "Arun", "Das", "arun.das@test.com", "9012345671");
            request.setPanNumber("JKLMN3456O");
            request.setAadhaarNumber("112345678901");
            CustomerResponse created = customerService.create(request);

            CustomerResponse blocked = customerService.block(
                    created.getId(), "Suspicious activity");

            assertThat(blocked.getStatus()).isEqualTo("BLOCKED");
        }
    }

    // ==================== Search Tests ====================

    @Nested
    @DisplayName("Customer Search")
    class SearchTests {

        @Test
        @DisplayName("Should search customers by name")
        void shouldSearchByName() {
            // Create a customer with a unique name
            CustomerRequest request = buildCustomerRequest(
                    "Chandrasekhar", "Venkatesh", "chandrasekhar.v@test.com", "9234567890");
            request.setPanNumber("STUVW1234X");
            request.setAadhaarNumber("212345678901");
            customerService.create(request);

            Page<CustomerResponse> results = customerService.searchByName(
                    "Chandrasekhar", PageRequest.of(0, 10));

            assertThat(results.getContent()).isNotEmpty();
            assertThat(results.getContent().get(0).getFirstName()).isEqualTo("Chandrasekhar");
        }

        @Test
        @DisplayName("Should search customers by query across multiple fields")
        void shouldSearchByQuery() {
            CustomerRequest request = buildCustomerRequest(
                    "Karthik", "Raman", "karthik.raman@test.com", "9345678901");
            request.setPanNumber("YZABC5678D");
            request.setAadhaarNumber("312345678901");
            customerService.create(request);

            Page<CustomerResponse> results = customerService.searchByQuery(
                    "karthik", PageRequest.of(0, 10));

            assertThat(results.getContent()).isNotEmpty();
        }
    }
}
