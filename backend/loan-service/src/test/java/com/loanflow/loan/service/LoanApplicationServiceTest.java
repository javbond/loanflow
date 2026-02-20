package com.loanflow.loan.service;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.mapper.LoanApplicationMapper;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.service.impl.LoanApplicationServiceImpl;
import com.loanflow.loan.workflow.WorkflowService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Cases for LoanApplicationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationService Tests")
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private LoanApplicationMapper mapper;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private LoanApplicationServiceImpl service;

    private UUID applicationId;
    private UUID customerId;
    private LoanApplicationRequest createRequest;
    private LoanApplication loanApplication;
    private LoanApplicationResponse expectedResponse;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        createRequest = LoanApplicationRequest.builder()
                .customerId(customerId)
                .loanType("HOME_LOAN")
                .requestedAmount(new BigDecimal("5000000"))
                .tenureMonths(240)
                .purpose("Purchase of residential property")
                .branchCode("MUM001")
                .build();

        loanApplication = LoanApplication.builder()
                .id(applicationId)
                .applicationNumber("LN-2024-000001")
                .customerId(customerId)
                .loanType(LoanType.HOME_LOAN)
                .requestedAmount(new BigDecimal("5000000"))
                .tenureMonths(240)
                .status(LoanStatus.DRAFT)
                .purpose("Purchase of residential property")
                .branchCode("MUM001")
                .build();

        expectedResponse = LoanApplicationResponse.builder()
                .id(applicationId)
                .applicationNumber("LN-2024-000001")
                .customerId(customerId)
                .loanType("HOME_LOAN")
                .requestedAmount(new BigDecimal("5000000"))
                .status("DRAFT")
                .build();
    }

    @Nested
    @DisplayName("Create Loan Application")
    class CreateTests {

        @Test
        @DisplayName("Should create loan application successfully")
        void shouldCreateLoanApplication() {
            // Given
            when(mapper.toEntity(createRequest)).thenReturn(loanApplication);
            when(repository.save(any(LoanApplication.class))).thenReturn(loanApplication);
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            LoanApplicationResponse result = service.create(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getApplicationNumber()).isNotNull();
            assertThat(result.getStatus()).isEqualTo("DRAFT");

            // Verify application number was generated
            ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getApplicationNumber()).startsWith("LN-");
        }

        @Test
        @DisplayName("Should validate loan application on creation")
        void shouldValidateOnCreation() {
            // Given invalid request (amount too low)
            LoanApplicationRequest invalidRequest = LoanApplicationRequest.builder()
                    .customerId(customerId)
                    .loanType("PERSONAL_LOAN")
                    .requestedAmount(new BigDecimal("5000")) // Below minimum
                    .tenureMonths(24)
                    .purpose("Test")
                    .build();

            LoanApplication invalidApp = LoanApplication.builder()
                    .customerId(customerId)
                    .loanType(LoanType.PERSONAL_LOAN)
                    .requestedAmount(new BigDecimal("5000"))
                    .tenureMonths(24)
                    .build();

            when(mapper.toEntity(invalidRequest)).thenReturn(invalidApp);

            // When/Then
            assertThatThrownBy(() -> service.create(invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Minimum loan amount");
        }
    }

    @Nested
    @DisplayName("Get Loan Application")
    class GetTests {

        @Test
        @DisplayName("Should get loan application by ID")
        void shouldGetById() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.of(loanApplication));
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            LoanApplicationResponse result = service.getById(applicationId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(applicationId);
        }

        @Test
        @DisplayName("Should throw exception when application not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(applicationId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LoanApplication");
        }

        @Test
        @DisplayName("Should get loan application by application number")
        void shouldGetByApplicationNumber() {
            // Given
            String appNumber = "LN-2024-000001";
            when(repository.findByApplicationNumber(appNumber)).thenReturn(Optional.of(loanApplication));
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            LoanApplicationResponse result = service.getByApplicationNumber(appNumber);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getApplicationNumber()).isEqualTo(appNumber);
        }
    }

    @Nested
    @DisplayName("List Loan Applications")
    class ListTests {

        @Test
        @DisplayName("Should list all applications with pagination")
        void shouldListWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<LoanApplication> page = new PageImpl<>(List.of(loanApplication), pageable, 1);

            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            Page<LoanApplicationResponse> result = service.getAll(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should list applications by customer ID")
        void shouldListByCustomerId() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<LoanApplication> page = new PageImpl<>(List.of(loanApplication), pageable, 1);

            when(repository.findByCustomerId(customerId, pageable)).thenReturn(page);
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            Page<LoanApplicationResponse> result = service.getByCustomerId(customerId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("Should list applications by status")
        void shouldListByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<LoanApplication> page = new PageImpl<>(List.of(loanApplication), pageable, 1);

            when(repository.findByStatus(LoanStatus.DRAFT, pageable)).thenReturn(page);
            when(mapper.toResponse(loanApplication)).thenReturn(expectedResponse);

            // When
            Page<LoanApplicationResponse> result = service.getByStatus(LoanStatus.DRAFT, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Submit Loan Application")
    class SubmitTests {

        @Test
        @DisplayName("Should submit draft application and start workflow")
        void shouldSubmitDraftApplication() {
            // Given
            when(repository.findById(applicationId)).thenReturn(Optional.of(loanApplication));
            when(repository.save(any(LoanApplication.class))).thenReturn(loanApplication);
            when(workflowService.startProcess(any(UUID.class), anyMap())).thenReturn("proc-123");

            LoanApplicationResponse submittedResponse = LoanApplicationResponse.builder()
                    .id(applicationId)
                    .status("SUBMITTED")
                    .build();
            when(mapper.toResponse(any(LoanApplication.class))).thenReturn(submittedResponse);

            // When
            LoanApplicationResponse result = service.submit(applicationId);

            // Then
            assertThat(result.getStatus()).isEqualTo("SUBMITTED");

            verify(workflowService).startProcess(eq(applicationId), anyMap());
            verify(repository, atLeast(2)).save(any(LoanApplication.class));
        }

        @Test
        @DisplayName("Should not submit non-draft application")
        void shouldNotSubmitNonDraft() {
            // Given
            loanApplication.setStatus(LoanStatus.APPROVED);
            when(repository.findById(applicationId)).thenReturn(Optional.of(loanApplication));

            // When/Then
            assertThatThrownBy(() -> service.submit(applicationId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Approve/Reject Loan Application")
    class DecisionTests {

        @BeforeEach
        void moveToUnderwriting() {
            loanApplication.setStatus(LoanStatus.UNDERWRITING);
        }

        @Test
        @DisplayName("Should approve application")
        void shouldApproveApplication() {
            // Given
            BigDecimal approvedAmount = new BigDecimal("4500000");
            BigDecimal interestRate = new BigDecimal("8.75");

            when(repository.findById(applicationId)).thenReturn(Optional.of(loanApplication));
            when(repository.save(any(LoanApplication.class))).thenReturn(loanApplication);

            LoanApplicationResponse approvedResponse = LoanApplicationResponse.builder()
                    .id(applicationId)
                    .status("APPROVED")
                    .approvedAmount(approvedAmount)
                    .interestRate(interestRate)
                    .build();
            when(mapper.toResponse(any(LoanApplication.class))).thenReturn(approvedResponse);

            // When
            LoanApplicationResponse result = service.approve(applicationId, approvedAmount, interestRate);

            // Then
            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getApprovedAmount()).isEqualTo(approvedAmount);

            ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEmiAmount()).isNotNull();
        }

        @Test
        @DisplayName("Should reject application with reason")
        void shouldRejectApplication() {
            // Given
            String reason = "CIBIL score below threshold";
            when(repository.findById(applicationId)).thenReturn(Optional.of(loanApplication));
            when(repository.save(any(LoanApplication.class))).thenReturn(loanApplication);

            LoanApplicationResponse rejectedResponse = LoanApplicationResponse.builder()
                    .id(applicationId)
                    .status("REJECTED")
                    .build();
            when(mapper.toResponse(any(LoanApplication.class))).thenReturn(rejectedResponse);

            // When
            LoanApplicationResponse result = service.reject(applicationId, reason);

            // Then
            assertThat(result.getStatus()).isEqualTo("REJECTED");

            ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getRejectionReason()).isEqualTo(reason);
        }
    }
}
