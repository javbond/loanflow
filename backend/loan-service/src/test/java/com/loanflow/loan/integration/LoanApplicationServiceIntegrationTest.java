package com.loanflow.loan.integration;

import com.loanflow.dto.request.LoanApplicationRequest;
import com.loanflow.dto.response.LoanApplicationResponse;
import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.notification.NotificationPublisher;
import com.loanflow.loan.service.LoanApplicationService;
import com.loanflow.util.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for LoanApplicationService with real PostgreSQL via Testcontainers.
 * Tests CRUD operations, status transitions, and workflow integration
 * against an actual database with Flyway migrations and Flowable engine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanApplicationServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private LoanApplicationService loanService;

    // Mock infrastructure beans — no real RabbitMQ/Redis in integration tests
    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private NotificationPublisher notificationPublisher;

    // ==================== Test Data Builders ====================

    private LoanApplicationRequest buildHomeLoanRequest() {
        return LoanApplicationRequest.builder()
                .customerId(UUID.randomUUID())
                .loanType("HOME_LOAN")
                .requestedAmount(new BigDecimal("5000000"))
                .tenureMonths(240)
                .purpose("Purchase of residential property in Bangalore")
                .branchCode("BLR001")
                .employmentDetails(LoanApplicationRequest.EmploymentDetails.builder()
                        .employmentType("SALARIED")
                        .employerName("TechCorp India")
                        .monthlyIncome(new BigDecimal("150000"))
                        .yearsOfExperience(8)
                        .build())
                .propertyDetails(LoanApplicationRequest.PropertyDetails.builder()
                        .propertyType("APARTMENT")
                        .address("123 HSR Layout, Sector 5")
                        .city("Bangalore")
                        .state("Karnataka")
                        .pinCode("560102")
                        .estimatedValue(new BigDecimal("6000000"))
                        .build())
                .build();
    }

    private LoanApplicationRequest buildPersonalLoanRequest() {
        return LoanApplicationRequest.builder()
                .customerId(UUID.randomUUID())
                .loanType("PERSONAL_LOAN")
                .requestedAmount(new BigDecimal("500000"))
                .tenureMonths(36)
                .purpose("Home renovation")
                .branchCode("MUM001")
                .employmentDetails(LoanApplicationRequest.EmploymentDetails.builder()
                        .employmentType("SALARIED")
                        .employerName("FinanceHub Ltd")
                        .monthlyIncome(new BigDecimal("80000"))
                        .yearsOfExperience(5)
                        .build())
                .build();
    }

    // ==================== Create Tests ====================

    @Nested
    @DisplayName("Loan Application Creation")
    class CreateTests {

        @Test
        @DisplayName("Should create home loan application in DRAFT status with auto-generated number")
        void shouldCreateHomeLoan() {
            LoanApplicationRequest request = buildHomeLoanRequest();

            LoanApplicationResponse response = loanService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getApplicationNumber()).startsWith("LN-");
            assertThat(response.getStatus()).isEqualTo("DRAFT");
            assertThat(response.getLoanType()).isEqualTo("HOME_LOAN");
            assertThat(response.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(response.getTenureMonths()).isEqualTo(240);
        }

        @Test
        @DisplayName("Should create personal loan application in DRAFT status")
        void shouldCreatePersonalLoan() {
            LoanApplicationRequest request = buildPersonalLoanRequest();

            LoanApplicationResponse response = loanService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getLoanType()).isEqualTo("PERSONAL_LOAN");
            assertThat(response.getStatus()).isEqualTo("DRAFT");
            assertThat(response.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("500000"));
        }

        @Test
        @DisplayName("Should generate unique application numbers for multiple applications")
        void shouldGenerateUniqueNumbers() {
            LoanApplicationResponse loan1 = loanService.create(buildHomeLoanRequest());
            LoanApplicationResponse loan2 = loanService.create(buildPersonalLoanRequest());

            assertThat(loan1.getApplicationNumber()).isNotEqualTo(loan2.getApplicationNumber());
        }
    }

    // ==================== Read Tests ====================

    @Nested
    @DisplayName("Loan Application Retrieval")
    class ReadTests {

        @Test
        @DisplayName("Should retrieve application by ID")
        void shouldGetById() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());

            LoanApplicationResponse retrieved = loanService.getById(created.getId());

            assertThat(retrieved.getId()).isEqualTo(created.getId());
            assertThat(retrieved.getApplicationNumber()).isEqualTo(created.getApplicationNumber());
        }

        @Test
        @DisplayName("Should retrieve application by application number")
        void shouldGetByApplicationNumber() {
            LoanApplicationResponse created = loanService.create(buildPersonalLoanRequest());

            LoanApplicationResponse retrieved = loanService.getByApplicationNumber(
                    created.getApplicationNumber());

            assertThat(retrieved.getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent ID")
        void shouldThrowForNonExistentId() {
            assertThatThrownBy(() -> loanService.getById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should paginate all applications")
        void shouldPaginateAll() {
            loanService.create(buildHomeLoanRequest());

            Page<LoanApplicationResponse> page = loanService.getAll(PageRequest.of(0, 10));

            assertThat(page).isNotNull();
            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should find applications by customer ID")
        void shouldFindByCustomerId() {
            UUID customerId = UUID.randomUUID();
            LoanApplicationRequest request = buildHomeLoanRequest();
            request.setCustomerId(customerId);
            loanService.create(request);

            Page<LoanApplicationResponse> page = loanService.getByCustomerId(
                    customerId, PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getCustomerId()).isEqualTo(customerId);
        }
    }

    // ==================== Status Transition Tests ====================

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should submit draft application — Flowable workflow auto-advances to DOCUMENT_VERIFICATION")
        void shouldSubmitApplication() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());

            LoanApplicationResponse submitted = loanService.submit(created.getId());

            // Flowable BPMN workflow starts on submit and the status-sync task listener
            // auto-advances the status to DOCUMENT_VERIFICATION when the first user task is created
            assertThat(submitted.getStatus()).isIn("SUBMITTED", "DOCUMENT_VERIFICATION");
        }

        @Test
        @DisplayName("Should cancel draft application")
        void shouldCancelDraftApplication() {
            LoanApplicationResponse created = loanService.create(buildPersonalLoanRequest());

            loanService.cancel(created.getId(), "Customer changed their mind");

            LoanApplicationResponse cancelled = loanService.getById(created.getId());
            assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should reject application from DOCUMENT_VERIFICATION status")
        void shouldRejectApplication() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());
            loanService.submit(created.getId());

            // After submit, Flowable advances to DOCUMENT_VERIFICATION.
            // reject() is valid from any non-terminal status, so we can reject directly.
            LoanApplicationResponse rejected = loanService.reject(
                    created.getId(), "Income insufficient");

            assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("Should approve application after reaching UNDERWRITING status")
        void shouldApproveApplication() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());
            loanService.submit(created.getId());

            // After submit, Flowable auto-advances to DOCUMENT_VERIFICATION.
            // Follow the valid transition chain: DOCUMENT_VERIFICATION → CREDIT_CHECK → UNDERWRITING
            loanService.transitionStatus(created.getId(), LoanStatus.CREDIT_CHECK);
            loanService.transitionStatus(created.getId(), LoanStatus.UNDERWRITING);

            LoanApplicationResponse approved = loanService.approve(
                    created.getId(),
                    new BigDecimal("4500000"),
                    new BigDecimal("8.75"));

            assertThat(approved.getStatus()).isEqualTo("APPROVED");
            assertThat(approved.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("4500000"));
            assertThat(approved.getInterestRate()).isEqualByComparingTo(new BigDecimal("8.75"));
        }
    }

    // ==================== Update Tests ====================

    @Nested
    @DisplayName("Loan Application Update")
    class UpdateTests {

        @Test
        @DisplayName("Should update draft application details")
        void shouldUpdateDraftApplication() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());

            LoanApplicationRequest updateRequest = buildHomeLoanRequest();
            updateRequest.setRequestedAmount(new BigDecimal("6000000"));
            updateRequest.setPurpose("Updated: Larger property");

            LoanApplicationResponse updated = loanService.update(
                    created.getId(), updateRequest);

            assertThat(updated.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("6000000"));
        }
    }

    // ==================== Search Tests ====================

    @Nested
    @DisplayName("Application Search")
    class SearchTests {

        @Test
        @DisplayName("Should search by application number prefix")
        void shouldSearchByApplicationNumber() {
            LoanApplicationResponse created = loanService.create(buildHomeLoanRequest());

            Page<LoanApplicationResponse> results = loanService.searchByApplicationNumber(
                    created.getApplicationNumber().substring(0, 7), PageRequest.of(0, 10));

            assertThat(results.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should find applications by status")
        void shouldFindByStatus() {
            loanService.create(buildHomeLoanRequest());

            Page<LoanApplicationResponse> drafts = loanService.getByStatus(
                    LoanStatus.DRAFT, PageRequest.of(0, 10));

            assertThat(drafts.getContent()).isNotEmpty();
            assertThat(drafts.getContent()).allMatch(app -> "DRAFT".equals(app.getStatus()));
        }
    }
}
