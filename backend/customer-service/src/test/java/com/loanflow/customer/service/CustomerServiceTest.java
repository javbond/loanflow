package com.loanflow.customer.service;

import com.loanflow.customer.domain.entity.Customer;
import com.loanflow.customer.domain.enums.CustomerStatus;
import com.loanflow.customer.domain.enums.Gender;
import com.loanflow.customer.domain.enums.KycStatus;
import com.loanflow.customer.mapper.CustomerMapper;
import com.loanflow.customer.repository.CustomerRepository;
import com.loanflow.customer.service.impl.CustomerServiceImpl;
import com.loanflow.dto.request.CustomerRequest;
import com.loanflow.dto.response.CustomerResponse;
import com.loanflow.util.exception.DuplicateResourceException;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test Cases for CustomerService
 * Tests written FIRST, then implementation follows
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    @InjectMocks
    private CustomerServiceImpl service;

    private UUID customerId;
    private CustomerRequest createRequest;
    private Customer customer;
    private CustomerResponse expectedResponse;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        createRequest = CustomerRequest.builder()
                .firstName("Rahul")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("MALE")
                .email("rahul.sharma@email.com")
                .mobileNumber("9876543210")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .customerNumber("CUS-2024-000001")
                .firstName("Rahul")
                .lastName("Sharma")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender(Gender.MALE)
                .email("rahul.sharma@email.com")
                .mobileNumber("9876543210")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("123456789012")
                .status(CustomerStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING)
                .build();

        expectedResponse = CustomerResponse.builder()
                .id(customerId)
                .customerNumber("CUS-2024-000001")
                .firstName("Rahul")
                .lastName("Sharma")
                .fullName("Rahul Sharma")
                .status("ACTIVE")
                .kycStatus("PENDING")
                .build();
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create customer successfully")
        void shouldCreateCustomer() {
            // Given
            when(repository.existsByPanNumber(createRequest.getPanNumber())).thenReturn(false);
            when(repository.existsByAadhaarNumber(createRequest.getAadhaarNumber())).thenReturn(false);
            when(repository.existsByEmail(createRequest.getEmail())).thenReturn(false);
            when(repository.existsByMobileNumber(createRequest.getMobileNumber())).thenReturn(false);
            when(mapper.toEntity(createRequest)).thenReturn(customer);
            when(repository.save(any(Customer.class))).thenReturn(customer);
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            CustomerResponse result = service.create(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerNumber()).startsWith("CUS-");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCustomerNumber()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception for duplicate PAN")
        void shouldThrowForDuplicatePan() {
            // Given
            when(repository.existsByPanNumber(createRequest.getPanNumber())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("PAN");
        }

        @Test
        @DisplayName("Should throw exception for duplicate Aadhaar")
        void shouldThrowForDuplicateAadhaar() {
            // Given
            when(repository.existsByPanNumber(any())).thenReturn(false);
            when(repository.existsByAadhaarNumber(createRequest.getAadhaarNumber())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Aadhaar");
        }

        @Test
        @DisplayName("Should throw exception for duplicate email")
        void shouldThrowForDuplicateEmail() {
            // Given
            when(repository.existsByPanNumber(any())).thenReturn(false);
            when(repository.existsByAadhaarNumber(any())).thenReturn(false);
            when(repository.existsByEmail(createRequest.getEmail())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("Should throw exception for duplicate mobile")
        void shouldThrowForDuplicateMobile() {
            // Given
            when(repository.existsByPanNumber(any())).thenReturn(false);
            when(repository.existsByAadhaarNumber(any())).thenReturn(false);
            when(repository.existsByEmail(any())).thenReturn(false);
            when(repository.existsByMobileNumber(createRequest.getMobileNumber())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.create(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("mobile");
        }

        @Test
        @DisplayName("Should validate customer data on creation")
        void shouldValidateOnCreation() {
            // Given - minor customer
            CustomerRequest minorRequest = CustomerRequest.builder()
                    .firstName("Minor")
                    .lastName("Customer")
                    .dateOfBirth(LocalDate.now().minusYears(15))
                    .gender("MALE")
                    .email("minor@email.com")
                    .mobileNumber("9876543211")
                    .build();

            Customer minorCustomer = Customer.builder()
                    .firstName("Minor")
                    .lastName("Customer")
                    .dateOfBirth(LocalDate.now().minusYears(15))
                    .gender(Gender.MALE)
                    .email("minor@email.com")
                    .mobileNumber("9876543211")
                    .build();

            when(repository.existsByPanNumber(any())).thenReturn(false);
            when(repository.existsByAadhaarNumber(any())).thenReturn(false);
            when(repository.existsByEmail(any())).thenReturn(false);
            when(repository.existsByMobileNumber(any())).thenReturn(false);
            when(mapper.toEntity(minorRequest)).thenReturn(minorCustomer);

            // When/Then
            assertThatThrownBy(() -> service.create(minorRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("18 years");
        }
    }

    @Nested
    @DisplayName("Get Customer Tests")
    class GetTests {

        @Test
        @DisplayName("Should get customer by ID")
        void shouldGetById() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            CustomerResponse result = service.getById(customerId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Should throw exception when customer not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer");
        }

        @Test
        @DisplayName("Should get customer by customer number")
        void shouldGetByCustomerNumber() {
            // Given
            String customerNumber = "CUS-2024-000001";
            when(repository.findByCustomerNumber(customerNumber)).thenReturn(Optional.of(customer));
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            CustomerResponse result = service.getByCustomerNumber(customerNumber);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerNumber()).isEqualTo(customerNumber);
        }

        @Test
        @DisplayName("Should get customer by PAN")
        void shouldGetByPan() {
            // Given
            String pan = "ABCDE1234F";
            when(repository.findByPanNumber(pan)).thenReturn(Optional.of(customer));
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            CustomerResponse result = service.getByPan(pan);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should get customer by mobile")
        void shouldGetByMobile() {
            // Given
            String mobile = "9876543210";
            when(repository.findByMobileNumber(mobile)).thenReturn(Optional.of(customer));
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            CustomerResponse result = service.getByMobile(mobile);

            // Then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("List Customers Tests")
    class ListTests {

        @Test
        @DisplayName("Should list all customers with pagination")
        void shouldListWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            Page<CustomerResponse> result = service.getAll(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should search customers by name")
        void shouldSearchByName() {
            // Given
            String searchTerm = "Rahul";
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

            when(repository.searchByName(searchTerm, pageable)).thenReturn(page);
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            Page<CustomerResponse> result = service.searchByName(searchTerm, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter customers by KYC status")
        void shouldFilterByKycStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Customer> page = new PageImpl<>(List.of(customer), pageable, 1);

            when(repository.findByKycStatus(KycStatus.PENDING, pageable)).thenReturn(page);
            when(mapper.toResponse(customer)).thenReturn(expectedResponse);

            // When
            Page<CustomerResponse> result = service.getByKycStatus(KycStatus.PENDING, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("KYC Verification Tests")
    class KycTests {

        @Test
        @DisplayName("Should verify Aadhaar")
        void shouldVerifyAadhaar() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse verifiedResponse = CustomerResponse.builder()
                    .id(customerId)
                    .aadhaarVerified(true)
                    .kycStatus("PARTIAL")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(verifiedResponse);

            // When
            CustomerResponse result = service.verifyAadhaar(customerId);

            // Then
            assertThat(result.getAadhaarVerified()).isTrue();

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAadhaarVerified()).isTrue();
        }

        @Test
        @DisplayName("Should verify PAN")
        void shouldVerifyPan() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse verifiedResponse = CustomerResponse.builder()
                    .id(customerId)
                    .panVerified(true)
                    .kycStatus("PARTIAL")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(verifiedResponse);

            // When
            CustomerResponse result = service.verifyPan(customerId);

            // Then
            assertThat(result.getPanVerified()).isTrue();
        }

        @Test
        @DisplayName("Should update KYC status to VERIFIED when both verified")
        void shouldUpdateKycToVerified() {
            // Given
            customer.setAadhaarVerified(true);
            customer.setPanVerified(true);

            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse verifiedResponse = CustomerResponse.builder()
                    .id(customerId)
                    .kycStatus("VERIFIED")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(verifiedResponse);

            // When
            CustomerResponse result = service.updateKycStatus(customerId);

            // Then
            assertThat(result.getKycStatus()).isEqualTo("VERIFIED");
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusTests {

        @Test
        @DisplayName("Should deactivate customer")
        void shouldDeactivateCustomer() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse inactiveResponse = CustomerResponse.builder()
                    .id(customerId)
                    .status("INACTIVE")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(inactiveResponse);

            // When
            CustomerResponse result = service.deactivate(customerId, "Customer request");

            // Then
            assertThat(result.getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("Should block customer")
        void shouldBlockCustomer() {
            // Given
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse blockedResponse = CustomerResponse.builder()
                    .id(customerId)
                    .status("BLOCKED")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(blockedResponse);

            // When
            CustomerResponse result = service.block(customerId, "Fraud detected");

            // Then
            assertThat(result.getStatus()).isEqualTo("BLOCKED");
        }

        @Test
        @DisplayName("Should reactivate inactive customer")
        void shouldReactivateCustomer() {
            // Given
            customer.setStatus(CustomerStatus.INACTIVE);
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));
            when(repository.save(any(Customer.class))).thenReturn(customer);

            CustomerResponse activeResponse = CustomerResponse.builder()
                    .id(customerId)
                    .status("ACTIVE")
                    .build();
            when(mapper.toResponse(any(Customer.class))).thenReturn(activeResponse);

            // When
            CustomerResponse result = service.reactivate(customerId);

            // Then
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should not reactivate blocked customer")
        void shouldNotReactivateBlocked() {
            // Given
            customer.setStatus(CustomerStatus.BLOCKED);
            when(repository.findById(customerId)).thenReturn(Optional.of(customer));

            // When/Then
            assertThatThrownBy(() -> service.reactivate(customerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("blocked");
        }
    }
}
