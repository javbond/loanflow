package com.loanflow.loan.repository;

import com.loanflow.loan.domain.entity.ApprovalAuthority;
import com.loanflow.loan.domain.enums.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalAuthorityRepository extends JpaRepository<ApprovalAuthority, UUID> {

    /**
     * Find all active tiers for a specific loan type, ordered by tier level.
     */
    List<ApprovalAuthority> findByLoanTypeAndActiveTrueOrderByTierLevel(LoanType loanType);

    /**
     * Find all active tiers that apply to all loan types (loanType IS NULL).
     */
    List<ApprovalAuthority> findByLoanTypeIsNullAndActiveTrueOrderByTierLevel();

    /**
     * Find the correct approval tier for a given loan type and amount.
     * Returns the tier where amount is between minAmount and maxAmount (or maxAmount is null).
     */
    @Query("""
            SELECT a FROM ApprovalAuthority a
            WHERE a.active = true
              AND (a.loanType = :loanType OR a.loanType IS NULL)
              AND a.minAmount <= :amount
              AND (a.maxAmount IS NULL OR a.maxAmount >= :amount)
            ORDER BY a.loanType DESC NULLS LAST, a.tierLevel ASC
            """)
    List<ApprovalAuthority> findMatchingTiers(
            @Param("loanType") LoanType loanType,
            @Param("amount") BigDecimal amount);

    /**
     * Find all active tiers ordered by loan type and tier level.
     */
    List<ApprovalAuthority> findByActiveTrueOrderByLoanTypeAscTierLevelAsc();

    /**
     * Find by loan type (including inactive).
     */
    List<ApprovalAuthority> findByLoanTypeOrderByTierLevel(LoanType loanType);

    /**
     * Check if a tier with the same loan type and level already exists.
     */
    Optional<ApprovalAuthority> findByLoanTypeAndTierLevel(LoanType loanType, int tierLevel);
}
