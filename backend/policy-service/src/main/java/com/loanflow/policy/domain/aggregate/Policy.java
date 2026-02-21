package com.loanflow.policy.domain.aggregate;

import com.loanflow.policy.domain.enums.LoanType;
import com.loanflow.policy.domain.enums.PolicyCategory;
import com.loanflow.policy.domain.enums.PolicyStatus;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Policy Aggregate Root - DDD aggregate for the policy engine.
 *
 * A Policy defines a set of rules that determine loan eligibility, pricing,
 * credit limits, document requirements, etc.
 *
 * Policies support versioning: when updated, a new version is created
 * while the previous version is archived.
 */
@Document(collection = "policies")
@CompoundIndex(name = "category_status_idx", def = "{'category': 1, 'status': 1}")
@CompoundIndex(name = "loan_type_status_idx", def = "{'loanType': 1, 'status': 1}")
@CompoundIndex(name = "code_version_idx", def = "{'policyCode': 1, 'versionNumber': -1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    @Id
    private String id;

    /**
     * Unique policy code (e.g., POL-2026-000001).
     * Stays the same across versions.
     */
    @Indexed
    @Field("policy_code")
    private String policyCode;

    /**
     * Human-readable name
     */
    @Field("name")
    private String name;

    /**
     * Detailed description of what this policy does
     */
    @Field("description")
    private String description;

    /**
     * Category of the policy
     */
    @Indexed
    @Field("category")
    private PolicyCategory category;

    /**
     * Which loan type this policy applies to
     */
    @Indexed
    @Field("loan_type")
    private LoanType loanType;

    /**
     * Current status
     */
    @Indexed
    @Field("status")
    @Builder.Default
    private PolicyStatus status = PolicyStatus.DRAFT;

    /**
     * Version number (increments on each update)
     */
    @Field("version_number")
    @Builder.Default
    private Integer versionNumber = 1;

    /**
     * ID of the previous version (null for version 1)
     */
    @Field("previous_version_id")
    private String previousVersionId;

    /**
     * The policy rules (conditions + actions)
     */
    @Field("rules")
    @Builder.Default
    private List<PolicyRule> rules = new ArrayList<>();

    /**
     * Execution priority (lower = evaluated first).
     * When multiple policies match, they are applied in priority order.
     */
    @Field("priority")
    @Builder.Default
    private Integer priority = 100;

    /**
     * Effective start date (null = immediately on activation)
     */
    @Field("effective_from")
    private LocalDateTime effectiveFrom;

    /**
     * Effective end date (null = no expiry)
     */
    @Field("effective_until")
    private LocalDateTime effectiveUntil;

    /**
     * Tags for searching/filtering
     */
    @Field("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Who created this policy
     */
    @Field("created_by")
    private String createdBy;

    /**
     * Who last modified this policy
     */
    @Field("modified_by")
    private String modifiedBy;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Field("optimistic_lock_version")
    private Long lockVersion;

    // ==================== Business Methods ====================

    /**
     * Generate unique policy code
     */
    public static String generatePolicyCode() {
        long seq = SEQUENCE.incrementAndGet();
        return String.format("POL-%d-%06d", Year.now().getValue(), seq);
    }

    /**
     * Activate this policy
     */
    public void activate() {
        if (this.status == PolicyStatus.ACTIVE) {
            return; // Already active
        }
        if (this.rules == null || this.rules.isEmpty()) {
            throw new IllegalStateException("Cannot activate a policy with no rules");
        }
        this.status = PolicyStatus.ACTIVE;
    }

    /**
     * Deactivate this policy
     */
    public void deactivate() {
        this.status = PolicyStatus.INACTIVE;
    }

    /**
     * Archive this policy (typically when a new version replaces it)
     */
    public void archive() {
        this.status = PolicyStatus.ARCHIVED;
    }

    /**
     * Create a new version of this policy.
     * The current policy is archived, and a new draft copy is returned.
     */
    public Policy createNewVersion() {
        return Policy.builder()
                .policyCode(this.policyCode)
                .name(this.name)
                .description(this.description)
                .category(this.category)
                .loanType(this.loanType)
                .status(PolicyStatus.DRAFT)
                .versionNumber(this.versionNumber + 1)
                .previousVersionId(this.id)
                .rules(new ArrayList<>(this.rules))
                .priority(this.priority)
                .effectiveFrom(this.effectiveFrom)
                .effectiveUntil(this.effectiveUntil)
                .tags(new ArrayList<>(this.tags))
                .createdBy(this.modifiedBy)
                .build();
    }

    /**
     * Add a rule to this policy
     */
    public void addRule(PolicyRule rule) {
        if (this.status == PolicyStatus.ACTIVE || this.status == PolicyStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify an active or archived policy. Create a new version first.");
        }
        if (this.rules == null) {
            this.rules = new ArrayList<>();
        }
        this.rules.add(rule);
    }

    /**
     * Remove a rule by name
     */
    public boolean removeRule(String ruleName) {
        if (this.status == PolicyStatus.ACTIVE || this.status == PolicyStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify an active or archived policy. Create a new version first.");
        }
        return this.rules != null && this.rules.removeIf(r -> r.getName().equals(ruleName));
    }

    /**
     * Check if this policy is currently effective
     */
    public boolean isEffective() {
        if (this.status != PolicyStatus.ACTIVE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = (effectiveFrom == null || !now.isBefore(effectiveFrom));
        boolean beforeEnd = (effectiveUntil == null || !now.isAfter(effectiveUntil));
        return afterStart && beforeEnd;
    }

    /**
     * Get the number of rules
     */
    public int getRuleCount() {
        return rules != null ? rules.size() : 0;
    }

    /**
     * Get enabled rules only
     */
    public List<PolicyRule> getEnabledRules() {
        if (rules == null) return List.of();
        return rules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .sorted((a, b) -> a.getPriority().compareTo(b.getPriority()))
                .toList();
    }

    /**
     * Pre-persist hook
     */
    public void prePersist() {
        if (this.policyCode == null) {
            this.policyCode = generatePolicyCode();
        }
        if (this.status == null) {
            this.status = PolicyStatus.DRAFT;
        }
        if (this.versionNumber == null) {
            this.versionNumber = 1;
        }
    }
}
