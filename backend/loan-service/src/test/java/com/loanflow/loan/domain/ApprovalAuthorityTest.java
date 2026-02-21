package com.loanflow.loan.domain;

import com.loanflow.loan.domain.entity.ApprovalAuthority;
import com.loanflow.loan.domain.entity.DelegationOfAuthority;
import com.loanflow.loan.domain.enums.LoanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApprovalAuthority and DelegationOfAuthority entities.
 */
@DisplayName("Approval Authority Entity Tests")
class ApprovalAuthorityTest {

    @Nested
    @DisplayName("ApprovalAuthority.matchesAmount()")
    class MatchesAmount {

        @Test
        @DisplayName("Should match amount within range")
        void shouldMatchWithinRange() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("1000000"))).isTrue();
        }

        @Test
        @DisplayName("Should match at exact minimum boundary")
        void shouldMatchAtMinBoundary() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("500001"))).isTrue();
        }

        @Test
        @DisplayName("Should match at exact maximum boundary")
        void shouldMatchAtMaxBoundary() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("2500000"))).isTrue();
        }

        @Test
        @DisplayName("Should match with null maxAmount (unlimited)")
        void shouldMatchWithNullMaxAmount() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("10000001"))
                    .maxAmount(null)
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("999999999"))).isTrue();
        }

        @Test
        @DisplayName("Should NOT match below minimum")
        void shouldNotMatchBelowMin() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("500000"))).isFalse();
        }

        @Test
        @DisplayName("Should NOT match above maximum")
        void shouldNotMatchAboveMax() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(new BigDecimal("500001"))
                    .maxAmount(new BigDecimal("2500000"))
                    .build();

            assertThat(tier.matchesAmount(new BigDecimal("2500001"))).isFalse();
        }

        @Test
        @DisplayName("Should NOT match null amount")
        void shouldNotMatchNullAmount() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .minAmount(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("500000"))
                    .build();

            assertThat(tier.matchesAmount(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("ApprovalAuthority Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build with defaults")
        void shouldBuildWithDefaults() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .tierLevel(1)
                    .tierName("Test")
                    .requiredRole("LOAN_OFFICER")
                    .build();

            assertThat(tier.getMinAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(tier.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should build global tier (null loanType)")
        void shouldBuildGlobalTier() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .loanType(null)
                    .tierLevel(1)
                    .tierName("Global Tier")
                    .requiredRole("LOAN_OFFICER")
                    .build();

            assertThat(tier.getLoanType()).isNull();
        }

        @Test
        @DisplayName("Should build product-specific tier")
        void shouldBuildProductSpecificTier() {
            ApprovalAuthority tier = ApprovalAuthority.builder()
                    .loanType(LoanType.HOME_LOAN)
                    .tierLevel(2)
                    .tierName("HL Senior UW")
                    .requiredRole("SENIOR_UNDERWRITER")
                    .minAmount(new BigDecimal("5000001"))
                    .maxAmount(new BigDecimal("20000000"))
                    .build();

            assertThat(tier.getLoanType()).isEqualTo(LoanType.HOME_LOAN);
            assertThat(tier.getRequiredRole()).isEqualTo("SENIOR_UNDERWRITER");
        }
    }

    @Nested
    @DisplayName("DelegationOfAuthority.isCurrentlyValid()")
    class DelegationValidity {

        @Test
        @DisplayName("Should be valid within date range and active")
        void shouldBeValidWithinRangeAndActive() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now().minusDays(1))
                    .validTo(LocalDate.now().plusDays(1))
                    .active(true)
                    .build();

            assertThat(delegation.isCurrentlyValid()).isTrue();
        }

        @Test
        @DisplayName("Should be valid on exact start date")
        void shouldBeValidOnStartDate() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now())
                    .validTo(LocalDate.now().plusDays(7))
                    .active(true)
                    .build();

            assertThat(delegation.isCurrentlyValid()).isTrue();
        }

        @Test
        @DisplayName("Should be invalid when inactive")
        void shouldBeInvalidWhenInactive() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now().minusDays(1))
                    .validTo(LocalDate.now().plusDays(1))
                    .active(false)
                    .build();

            assertThat(delegation.isCurrentlyValid()).isFalse();
        }

        @Test
        @DisplayName("Should be invalid before start date")
        void shouldBeInvalidBeforeStartDate() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now().plusDays(1))
                    .validTo(LocalDate.now().plusDays(7))
                    .active(true)
                    .build();

            assertThat(delegation.isCurrentlyValid()).isFalse();
        }

        @Test
        @DisplayName("Should be invalid after end date")
        void shouldBeInvalidAfterEndDate() {
            DelegationOfAuthority delegation = DelegationOfAuthority.builder()
                    .delegatorId(UUID.randomUUID())
                    .delegateeId(UUID.randomUUID())
                    .validFrom(LocalDate.now().minusDays(7))
                    .validTo(LocalDate.now().minusDays(1))
                    .active(true)
                    .build();

            assertThat(delegation.isCurrentlyValid()).isFalse();
        }
    }
}
