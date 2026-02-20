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
}
