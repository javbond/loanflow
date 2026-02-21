package com.loanflow.loan.service;

import com.loanflow.loan.domain.entity.ApprovalAuthority;
import com.loanflow.loan.domain.entity.DelegationOfAuthority;
import com.loanflow.loan.domain.enums.LoanType;
import com.loanflow.loan.repository.ApprovalAuthorityRepository;
import com.loanflow.loan.repository.DelegationOfAuthorityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD unit tests for ApprovalHierarchyService.
 * Tests tier resolution, CRUD operations, and delegation management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalHierarchyService Tests")
class ApprovalHierarchyServiceTest {

    @Mock
    private ApprovalAuthorityRepository authorityRepository;

    @Mock
    private DelegationOfAuthorityRepository delegationRepository;

    @InjectMocks
    private ApprovalHierarchyService service;

    // ======================== TIER RESOLUTION ========================

    @Nested
    @DisplayName("Tier Resolution")
    class TierResolution {

        @Test
        @DisplayName("Should resolve LOAN_OFFICER for small personal loan (₹3L)")
        void shouldResolveLoanOfficerForSmallLoan() {
            BigDecimal amount = new BigDecimal("300000");
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .tierLevel(1)
                    .tierName("Loan Officer Approval")
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("500000"))
                    .requiredRole("LOAN_OFFICER")
                    .active(true)
                    .build();

            when(authorityRepository.findMatchingTiers(LoanType.PERSONAL_LOAN, amount))
                    .thenReturn(List.of(tier));

            String role = service.resolveRequiredRole(LoanType.PERSONAL_LOAN, amount);

            assertThat(role).isEqualTo("LOAN_OFFICER");
        }

        @Test
        @DisplayName("Should resolve UNDERWRITER for medium personal loan (₹15L)")
        void shouldResolveUnderwriterForMediumLoan() {
            BigDecimal amount = new BigDecimal("1500000");
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .tierLevel(2)
                    .tierName("Underwriter Approval")
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .requiredRole("UNDERWRITER")
                    .active(true)
                    .build();

            when(authorityRepository.findMatchingTiers(LoanType.PERSONAL_LOAN, amount))
                    .thenReturn(List.of(tier));

            String role = service.resolveRequiredRole(LoanType.PERSONAL_LOAN, amount);

            assertThat(role).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should resolve SENIOR_UNDERWRITER for large loan (₹75L)")
        void shouldResolveSeniorUnderwriterForLargeLoan() {
            BigDecimal amount = new BigDecimal("7500000");
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .tierLevel(3)
                    .tierName("Senior Underwriter Approval")
                    .minAmount(new BigDecimal("2500001"))
                    .maxAmount(new BigDecimal("10000000"))
                    .requiredRole("SENIOR_UNDERWRITER")
                    .active(true)
                    .build();

            when(authorityRepository.findMatchingTiers(LoanType.PERSONAL_LOAN, amount))
                    .thenReturn(List.of(tier));

            String role = service.resolveRequiredRole(LoanType.PERSONAL_LOAN, amount);

            assertThat(role).isEqualTo("SENIOR_UNDERWRITER");
        }

        @Test
        @DisplayName("Should resolve BRANCH_MANAGER for very large loan (₹2Cr)")
        void shouldResolveBranchManagerForVeryLargeLoan() {
            BigDecimal amount = new BigDecimal("20000000");
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .tierLevel(4)
                    .tierName("Branch Manager Approval")
                    .minAmount(new BigDecimal("10000001"))
                    .maxAmount(null)
                    .requiredRole("BRANCH_MANAGER")
                    .active(true)
                    .build();

            when(authorityRepository.findMatchingTiers(LoanType.PERSONAL_LOAN, amount))
                    .thenReturn(List.of(tier));

            String role = service.resolveRequiredRole(LoanType.PERSONAL_LOAN, amount);

            assertThat(role).isEqualTo("BRANCH_MANAGER");
        }

        @Test
        @DisplayName("Should use product-specific tier over global tier")
        void shouldPreferProductSpecificTier() {
            BigDecimal amount = new BigDecimal("3000000");
            // Product-specific (HOME_LOAN) — higher limit for underwriter
            ApprovalAuthority hlTier = ApprovalAuthority.builder()
                    .loanType(LoanType.HOME_LOAN)
                    .tierLevel(1)
                    .tierName("HL Underwriter Approval")
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("5000000"))
                    .requiredRole("UNDERWRITER")
                    .active(true)
                    .build();

            when(authorityRepository.findMatchingTiers(LoanType.HOME_LOAN, amount))
                    .thenReturn(List.of(hlTier));

            String role = service.resolveRequiredRole(LoanType.HOME_LOAN, amount);

            // Home loan at ₹30L should be UNDERWRITER (product-specific),
            // NOT SENIOR_UNDERWRITER (global tier)
            assertThat(role).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should return default role when no matching tier found")
        void shouldReturnDefaultWhenNoMatchingTier() {
            BigDecimal amount = new BigDecimal("500000");

            when(authorityRepository.findMatchingTiers(LoanType.EDUCATION_LOAN, amount))
                    .thenReturn(List.of());

            String role = service.resolveRequiredRole(LoanType.EDUCATION_LOAN, amount);

            assertThat(role).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should return default role for null loan type")
        void shouldReturnDefaultForNullLoanType() {
            String role = service.resolveRequiredRole(null, new BigDecimal("500000"));
            assertThat(role).isEqualTo("UNDERWRITER");
        }

        @Test
        @DisplayName("Should return default role for null amount")
        void shouldReturnDefaultForNullAmount() {
            String role = service.resolveRequiredRole(LoanType.PERSONAL_LOAN, null);
            assertThat(role).isEqualTo("UNDERWRITER");
        }
    }

    // ======================== AUTHORITY CRUD ========================

    @Nested
    @DisplayName("Authority CRUD")
    class AuthorityCrud {

        @Test
        @DisplayName("Should create a new approval tier")
        void shouldCreateNewTier() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .loanType(LoanType.GOLD_LOAN)
                    .tierLevel(1)
                    .tierName("Gold Loan Officer")
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("1000000"))
                    .requiredRole("LOAN_OFFICER")
                    .build();

            when(authorityRepository.findByLoanTypeAndTierLevel(LoanType.GOLD_LOAN, 1))
                    .thenReturn(Optional.empty());
            when(authorityRepository.save(any())).thenReturn(tier);

            ApprovalAuthority created = service.createTier(tier);

            assertThat(created.getTierName()).isEqualTo("Gold Loan Officer");
            verify(authorityRepository).save(tier);
        }

        @Test
        @DisplayName("Should reject duplicate tier level for same loan type")
        void shouldRejectDuplicateTierLevel() {
            ApprovalAuthority existing = ApprovalAuthority.builder()
                    .tierLevel(1)
                    .loanType(LoanType.PERSONAL_LOAN)
                    .build();

            when(authorityRepository.findByLoanTypeAndTierLevel(LoanType.PERSONAL_LOAN, 1))
                    .thenReturn(Optional.of(existing));

            ApprovalAuthority newTier = ApprovalAuthority.builder()
                    .loanType(LoanType.PERSONAL_LOAN)
                    .tierLevel(1)
                    .tierName("Duplicate")
                    .build();

            assertThatThrownBy(() -> service.createTier(newTier))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update an existing tier")
        void shouldUpdateExistingTier() {
            UUID tierId = UUID.randomUUID();
            ApprovalAuthority existing = ApprovalAuthority.builder()
                    .id(tierId)
                    .tierName("Old Name")
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("500000"))
                    .requiredRole("LOAN_OFFICER")
                    .active(true)
                    .build();

            when(authorityRepository.findById(tierId)).thenReturn(Optional.of(existing));
            when(authorityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApprovalAuthority updates = ApprovalAuthority.builder()
                    .tierName("Updated Name")
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("750000"))
                    .requiredRole("LOAN_OFFICER")
                    .active(true)
                    .build();

            ApprovalAuthority result = service.updateTier(tierId, updates);

            assertThat(result.getTierName()).isEqualTo("Updated Name");
            assertThat(result.getMaxAmount()).isEqualByComparingTo(new BigDecimal("750000"));
        }

        @Test
        @DisplayName("Should deactivate a tier (soft delete)")
        void shouldDeactivateTier() {
            UUID tierId = UUID.randomUUID();
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .id(tierId)
                    .tierName("Test Tier")
                    .active(true)
                    .build();

            when(authorityRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(authorityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deactivateTier(tierId);

            ArgumentCaptor<ApprovalAuthority> captor = ArgumentCaptor.forClass(ApprovalAuthority.class);
            verify(authorityRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw when updating non-existent tier")
        void shouldThrowWhenUpdatingNonExistentTier() {
            UUID tierId = UUID.randomUUID();
            when(authorityRepository.findById(tierId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTier(tierId, ApprovalAuthority.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ======================== DELEGATION MANAGEMENT ========================

    @Nested
    @DisplayName("Delegation Management")
    class DelegationManagement {

        @Test
        @DisplayName("Should create a valid delegation")
        void shouldCreateValidDelegation() {
            UUID delegatorId = UUID.randomUUID();
            UUID delegateeId = UUID.randomUUID();

            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(delegatorId)
                    .delegatorRole("BRANCH_MANAGER")
                    .delegateeId(delegateeId)
                    .delegateeRole("SENIOR_UNDERWRITER")
                    .maxAmount(new BigDecimal("10000000"))
                    .validFrom(LocalDate.now())
                    .validTo(LocalDate.now().plusDays(14))
                    .reason("Annual leave")
                    .build();

            when(delegationRepository.save(any())).thenReturn(delegation);

            DelegationOfAuthority created = service.createDelegation(delegation);

            assertThat(created.getDelegatorRole()).isEqualTo("BRANCH_MANAGER");
            assertThat(created.getDelegateeRole()).isEqualTo("SENIOR_UNDERWRITER");
            verify(delegationRepository).save(delegation);
        }

        @Test
        @DisplayName("Should reject delegation with invalid date range")
        void shouldRejectInvalidDateRange() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now().plusDays(10))
                    .validTo(LocalDate.now()) // Before validFrom
                    .build();

            assertThatThrownBy(() -> service.createDelegation(delegation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must be after validFrom");
        }

        @Test
        @DisplayName("Should reject self-delegation")
        void shouldRejectSelfDelegation() {
            UUID sameOfficer = UUID.randomUUID();
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(sameOfficer)
                    .delegateeId(sameOfficer)
                    .validFrom(LocalDate.now())
                    .validTo(LocalDate.now().plusDays(7))
                    .build();

            assertThatThrownBy(() -> service.createDelegation(delegation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different officers");
        }

        @Test
        @DisplayName("Should revoke a delegation")
        void shouldRevokeDelegation() {
            UUID delegationId = UUID.randomUUID();
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .id(delegationId)
                    .active(true)
                    .build();

            when(delegationRepository.findById(delegationId)).thenReturn(Optional.of(delegation));
            when(delegationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.revokeDelegation(delegationId);

            ArgumentCaptor<DelegationOfAuthority> captor = ArgumentCaptor.forClass(DelegationOfAuthority.class);
            verify(delegationRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Should check delegated authority")
        void shouldCheckDelegatedAuthority() {
            UUID officerId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("5000000");

            when(delegationRepository.hasDelegatedAuthority(officerId, LocalDate.now(), amount))
                    .thenReturn(true);

            boolean hasAuthority = service.hasDelegatedAuthority(officerId, amount);

            assertThat(hasAuthority).isTrue();
        }
    }
}
