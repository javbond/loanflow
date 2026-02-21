package com.loanflow.loan.repository;

import com.loanflow.loan.domain.entity.LoanApplication;
import com.loanflow.loan.domain.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends
        JpaRepository<LoanApplication, UUID>,
        JpaSpecificationExecutor<LoanApplication> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    Page<LoanApplication> findByCustomerId(UUID customerId, Pageable pageable);

    Page<LoanApplication> findByCustomerEmail(String customerEmail, Pageable pageable);

    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    Page<LoanApplication> findByBranchCode(String branchCode, Pageable pageable);

    Page<LoanApplication> findByAssignedOfficer(UUID officerId, Pageable pageable);

    @Query("SELECT la FROM LoanApplication la WHERE la.status = :status AND la.branchCode = :branchCode")
    Page<LoanApplication> findByStatusAndBranch(
            @Param("status") LoanStatus status,
            @Param("branchCode") String branchCode,
            Pageable pageable);

    @Query("SELECT la FROM LoanApplication la WHERE la.status IN :statuses")
    Page<LoanApplication> findByStatusIn(@Param("statuses") List<LoanStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(la) FROM LoanApplication la WHERE la.customerId = :customerId AND la.status NOT IN ('REJECTED', 'CANCELLED', 'CLOSED')")
    long countActiveByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT la FROM LoanApplication la WHERE la.status = :status AND la.createdAt < :cutoffTime")
    List<LoanApplication> findStaleApplications(
            @Param("status") LoanStatus status,
            @Param("cutoffTime") Instant cutoffTime);

    boolean existsByApplicationNumber(String applicationNumber);

    @Query("SELECT la FROM LoanApplication la WHERE LOWER(la.applicationNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<LoanApplication> searchByApplicationNumber(@Param("query") String query, Pageable pageable);

    // ==================== US-019: Risk Dashboard Queries ====================

    /**
     * Count applications by risk category (for risk tier breakdown chart).
     */
    @Query("SELECT la.riskCategory, COUNT(la) FROM LoanApplication la " +
            "WHERE la.riskCategory IS NOT NULL AND la.status NOT IN ('DRAFT', 'CANCELLED') " +
            "GROUP BY la.riskCategory")
    List<Object[]> countByRiskCategory();

    /**
     * Count applications by CIBIL score range (for score distribution chart).
     */
    @Query("SELECT " +
            "CASE " +
            "  WHEN la.cibilScore >= 750 THEN 'EXCELLENT' " +
            "  WHEN la.cibilScore >= 700 THEN 'GOOD' " +
            "  WHEN la.cibilScore >= 650 THEN 'FAIR' " +
            "  WHEN la.cibilScore >= 550 THEN 'BELOW_AVERAGE' " +
            "  ELSE 'POOR' " +
            "END, COUNT(la) " +
            "FROM LoanApplication la " +
            "WHERE la.cibilScore IS NOT NULL AND la.status NOT IN ('DRAFT', 'CANCELLED') " +
            "GROUP BY " +
            "CASE " +
            "  WHEN la.cibilScore >= 750 THEN 'EXCELLENT' " +
            "  WHEN la.cibilScore >= 700 THEN 'GOOD' " +
            "  WHEN la.cibilScore >= 650 THEN 'FAIR' " +
            "  WHEN la.cibilScore >= 550 THEN 'BELOW_AVERAGE' " +
            "  ELSE 'POOR' " +
            "END")
    List<Object[]> countByCibilScoreRange();

    /**
     * Count applications by status (for status overview).
     */
    @Query("SELECT la.status, COUNT(la) FROM LoanApplication la " +
            "WHERE la.status NOT IN ('DRAFT') " +
            "GROUP BY la.status")
    List<Object[]> countByStatus();

    /**
     * Get average CIBIL score.
     */
    @Query("SELECT AVG(la.cibilScore) FROM LoanApplication la " +
            "WHERE la.cibilScore IS NOT NULL AND la.status NOT IN ('DRAFT', 'CANCELLED')")
    Double findAverageCibilScore();

    /**
     * Count high-risk applications (riskCategory in HIGH, MEDIUM_HIGH or cibilScore < 650).
     */
    @Query("SELECT COUNT(la) FROM LoanApplication la " +
            "WHERE la.status NOT IN ('DRAFT', 'CANCELLED', 'REJECTED') " +
            "AND (la.riskCategory IN ('HIGH', 'MEDIUM_HIGH') OR la.cibilScore < 650)")
    long countHighRiskApplications();

    /**
     * Find applications with negative markers (NPA status or rejection for credit reasons).
     */
    @Query("SELECT la FROM LoanApplication la " +
            "WHERE (la.status = 'NPA' " +
            "   OR (la.status = 'REJECTED' AND la.riskCategory IN ('HIGH', 'MEDIUM_HIGH'))) " +
            "ORDER BY la.updatedAt DESC")
    List<LoanApplication> findNegativeMarkerApplications();

    /**
     * Get total disbursed amount by risk category.
     */
    @Query("SELECT la.riskCategory, SUM(la.approvedAmount) FROM LoanApplication la " +
            "WHERE la.riskCategory IS NOT NULL " +
            "AND la.status IN ('APPROVED', 'DISBURSEMENT_PENDING', 'DISBURSED', 'CLOSED') " +
            "GROUP BY la.riskCategory")
    List<Object[]> sumApprovedAmountByRiskCategory();

    /**
     * Count applications by loan type and risk category (for cross-analysis).
     */
    @Query("SELECT la.loanType, la.riskCategory, COUNT(la) FROM LoanApplication la " +
            "WHERE la.riskCategory IS NOT NULL AND la.status NOT IN ('DRAFT', 'CANCELLED') " +
            "GROUP BY la.loanType, la.riskCategory " +
            "ORDER BY la.loanType, la.riskCategory")
    List<Object[]> countByLoanTypeAndRiskCategory();
}
