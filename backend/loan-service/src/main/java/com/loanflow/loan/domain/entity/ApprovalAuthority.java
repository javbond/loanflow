package com.loanflow.loan.domain.entity;

import com.loanflow.loan.domain.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Defines the approval authority matrix — maps loan amount ranges to required
 * approval roles. Each row represents a tier in the delegation of authority.
 *
 * Example tiers for PERSONAL_LOAN:
 *   ≤ ₹5,00,000  → LOAN_OFFICER
 *   ≤ ₹25,00,000 → UNDERWRITER
 *   ≤ ₹1,00,00,000 → SENIOR_UNDERWRITER
 *   > ₹1,00,00,000 → BRANCH_MANAGER
 */
@Entity
@Table(name = "approval_authority", schema = "application",
        indexes = {
                @Index(name = "idx_approval_auth_loan_type", columnList = "loan_type"),
                @Index(name = "idx_approval_auth_role", columnList = "required_role"),
                @Index(name = "idx_approval_auth_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_approval_auth_type_tier",
                        columnNames = {"loan_type", "tier_level"})
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Loan product type this tier applies to. NULL means applies to ALL loan types.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", length = 30)
    private LoanType loanType;

    /**
     * Tier level for ordering (1 = lowest authority, higher = more authority).
     */
    @Column(name = "tier_level", nullable = false)
    private int tierLevel;

    /**
     * Human-readable tier name.
     */
    @Column(name = "tier_name", nullable = false, length = 50)
    private String tierName;

    /**
     * Minimum loan amount (inclusive) for this tier. 0 for the lowest tier.
     */
    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minAmount = BigDecimal.ZERO;

    /**
     * Maximum loan amount (inclusive) for this tier. NULL means no upper limit.
     */
    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    /**
     * The Flowable candidate group / Keycloak role required for approval.
     * Must match a key in loanflow.assignment.officers YAML config.
     */
    @Column(name = "required_role", nullable = false, length = 30)
    private String requiredRole;

    /**
     * Whether this tier is currently active.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Check if a given amount falls within this tier's range.
     */
    public boolean matchesAmount(BigDecimal amount) {
        if (amount == null) return false;
        boolean aboveMin = amount.compareTo(minAmount) >= 0;
        boolean belowMax = maxAmount == null || amount.compareTo(maxAmount) <= 0;
        return aboveMin && belowMax;
    }
}
