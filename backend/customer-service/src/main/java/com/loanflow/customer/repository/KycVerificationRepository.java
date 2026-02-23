package com.loanflow.customer.repository;

import com.loanflow.customer.domain.entity.KycVerification;
import com.loanflow.customer.domain.enums.EkycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for KYC verification records (US-029).
 */
public interface KycVerificationRepository extends JpaRepository<KycVerification, UUID> {

    List<KycVerification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<KycVerification> findFirstByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<KycVerification> findByTransactionId(String transactionId);

    List<KycVerification> findByCustomerIdAndStatus(UUID customerId, EkycStatus status);

    long countByCustomerIdAndStatusIn(UUID customerId, List<EkycStatus> statuses);
}
