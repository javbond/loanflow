package com.loanflow.loan.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Temporary delegation of authority — allows a senior officer to delegate
 * their approval authority to another officer for a specific period.
 *
 * Example: Branch Manager on leave delegates authority to Senior Underwriter
 * for loans up to ₹1Cr from 2025-03-01 to 2025-03-15.
 */
@Entity
@Table(name = "delegation_of_authority", schema = "application",
        indexes = {
                @Index(name = "idx_doa_delegatee", columnList = "delegatee_id"),
                @Index(name = "idx_doa_delegator", columnList = "delegator_id"),
                @Index(name = "idx_doa_active", columnList = "active"),
                @Index(name = "idx_doa_valid_period", columnList = "valid_from, valid_to")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegationOfAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The officer granting the delegation (e.g., Branch Manager).
     */
    @Column(name = "delegator_id", nullable = false)
    private UUID delegatorId;

    /**
     * The role of the delegator.
     */
    @Column(name = "delegator_role", nullable = false, length = 30)
    private String delegatorRole;

    /**
     * The officer receiving the delegated authority.
     */
    @Column(name = "delegatee_id", nullable = false)
    private UUID delegateeId;

    /**
     * The role of the delegatee.
     */
    @Column(name = "delegatee_role", nullable = false, length = 30)
    private String delegateeRole;

    /**
     * Maximum amount the delegatee can approve under this delegation.
     */
    @Column(name = "max_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    /**
     * Start date of delegation validity.
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * End date of delegation validity.
     */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    /**
     * Reason for delegation (e.g., "Annual leave", "Training").
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Whether this delegation is currently active.
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

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /**
     * Check if this delegation is currently valid (active and within date range).
     */
    public boolean isCurrentlyValid() {
        if (!active) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(validFrom) && !today.isAfter(validTo);
    }
}
