package com.loanflow.loan.repository;

import com.loanflow.loan.domain.entity.DelegationOfAuthority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DelegationOfAuthorityRepository extends JpaRepository<DelegationOfAuthority, UUID> {

    /**
     * Find all currently valid delegations for a specific delegatee.
     */
    @Query("""
            SELECT d FROM DelegationOfAuthority d
            WHERE d.delegateeId = :delegateeId
              AND d.active = true
              AND d.validFrom <= :today
              AND d.validTo >= :today
            """)
    List<DelegationOfAuthority> findActiveDelegationsForDelegatee(
            @Param("delegateeId") UUID delegateeId,
            @Param("today") LocalDate today);

    /**
     * Find all active delegations by delegator.
     */
    List<DelegationOfAuthority> findByDelegatorIdAndActiveTrue(UUID delegatorId);

    /**
     * Find all active delegations.
     */
    List<DelegationOfAuthority> findByActiveTrue();

    /**
     * Check if a delegatee has authority (via delegation) for a given amount.
     */
    @Query("""
            SELECT COUNT(d) > 0 FROM DelegationOfAuthority d
            WHERE d.delegateeId = :delegateeId
              AND d.active = true
              AND d.validFrom <= :today
              AND d.validTo >= :today
              AND d.maxAmount >= :amount
            """)
    boolean hasDelegatedAuthority(
            @Param("delegateeId") UUID delegateeId,
            @Param("today") LocalDate today,
            @Param("amount") BigDecimal amount);
}
