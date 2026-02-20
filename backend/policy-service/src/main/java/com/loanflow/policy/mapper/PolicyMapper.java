package com.loanflow.policy.mapper;

import com.loanflow.dto.request.PolicyRequest;
import com.loanflow.dto.response.PolicyResponse;
import com.loanflow.policy.domain.aggregate.Policy;
import com.loanflow.policy.domain.enums.*;
import com.loanflow.policy.domain.valueobject.Action;
import com.loanflow.policy.domain.valueobject.Condition;
import com.loanflow.policy.domain.valueobject.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for Policy <-> DTO conversion.
 * Using manual mapping instead of MapStruct due to complex nested structures with enums.
 */
@Component
public class PolicyMapper {

    // ==================== Entity to Response ====================

    public PolicyResponse toResponse(Policy policy) {
        if (policy == null) return null;

        return PolicyResponse.builder()
                .id(policy.getId())
                .policyCode(policy.getPolicyCode())
                .name(policy.getName())
                .description(policy.getDescription())
                .category(policy.getCategory() != null ? policy.getCategory().name() : null)
                .loanType(policy.getLoanType() != null ? policy.getLoanType().name() : null)
                .status(policy.getStatus() != null ? policy.getStatus().name() : null)
                .versionNumber(policy.getVersionNumber())
                .previousVersionId(policy.getPreviousVersionId())
                .priority(policy.getPriority())
                .effectiveFrom(policy.getEffectiveFrom())
                .effectiveUntil(policy.getEffectiveUntil())
                .tags(policy.getTags())
                .rules(toRuleResponses(policy.getRules()))
                .ruleCount(policy.getRuleCount())
                .effective(policy.isEffective())
                .createdBy(policy.getCreatedBy())
                .modifiedBy(policy.getModifiedBy())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    public List<PolicyResponse> toResponseList(List<Policy> policies) {
        if (policies == null) return List.of();
        return policies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ==================== Request to Entity ====================

    public Policy toEntity(PolicyRequest request) {
        if (request == null) return null;

        Policy policy = Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(parseEnum(PolicyCategory.class, request.getCategory()))
                .loanType(parseEnum(LoanType.class, request.getLoanType()))
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveUntil(request.getEffectiveUntil())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .rules(toRuleEntities(request.getRules()))
                .build();

        policy.prePersist();
        return policy;
    }

    /**
     * Update existing entity from request (preserves id, code, version, etc.)
     */
    public void updateEntity(Policy policy, PolicyRequest request) {
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setCategory(parseEnum(PolicyCategory.class, request.getCategory()));
        policy.setLoanType(parseEnum(LoanType.class, request.getLoanType()));
        if (request.getPriority() != null) {
            policy.setPriority(request.getPriority());
        }
        policy.setEffectiveFrom(request.getEffectiveFrom());
        policy.setEffectiveUntil(request.getEffectiveUntil());
        if (request.getTags() != null) {
            policy.setTags(request.getTags());
        }
        if (request.getRules() != null) {
            policy.setRules(toRuleEntities(request.getRules()));
        }
    }

    // ==================== Rule Mapping ====================

    private List<PolicyResponse.PolicyRuleResponse> toRuleResponses(List<PolicyRule> rules) {
        if (rules == null) return List.of();
        return rules.stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    private PolicyResponse.PolicyRuleResponse toRuleResponse(PolicyRule rule) {
        return PolicyResponse.PolicyRuleResponse.builder()
                .name(rule.getName())
                .description(rule.getDescription())
                .logicalOperator(rule.getLogicalOperator() != null ? rule.getLogicalOperator().name() : null)
                .conditions(toConditionResponses(rule.getConditions()))
                .actions(toActionResponses(rule.getActions()))
                .priority(rule.getPriority())
                .enabled(rule.getEnabled())
                .build();
    }

    private List<PolicyRule> toRuleEntities(List<PolicyRequest.PolicyRuleRequest> rules) {
        if (rules == null) return new ArrayList<>();
        return rules.stream().map(this::toRuleEntity).collect(Collectors.toList());
    }

    private PolicyRule toRuleEntity(PolicyRequest.PolicyRuleRequest request) {
        return PolicyRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .logicalOperator(parseEnum(LogicalOperator.class, request.getLogicalOperator()))
                .conditions(toConditionEntities(request.getConditions()))
                .actions(toActionEntities(request.getActions()))
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
    }

    // ==================== Condition Mapping ====================

    private List<PolicyResponse.ConditionResponse> toConditionResponses(List<Condition> conditions) {
        if (conditions == null) return List.of();
        return conditions.stream().map(c -> PolicyResponse.ConditionResponse.builder()
                .field(c.getField())
                .operator(c.getOperator() != null ? c.getOperator().name() : null)
                .value(c.getValue())
                .values(c.getValues())
                .minValue(c.getMinValue())
                .maxValue(c.getMaxValue())
                .build()).collect(Collectors.toList());
    }

    private List<Condition> toConditionEntities(List<PolicyRequest.ConditionRequest> conditions) {
        if (conditions == null) return new ArrayList<>();
        return conditions.stream().map(c -> Condition.builder()
                .field(c.getField())
                .operator(parseEnum(ConditionOperator.class, c.getOperator()))
                .value(c.getValue())
                .values(c.getValues())
                .minValue(c.getMinValue())
                .maxValue(c.getMaxValue())
                .build()).collect(Collectors.toList());
    }

    // ==================== Action Mapping ====================

    private List<PolicyResponse.ActionResponse> toActionResponses(List<Action> actions) {
        if (actions == null) return List.of();
        return actions.stream().map(a -> PolicyResponse.ActionResponse.builder()
                .type(a.getType() != null ? a.getType().name() : null)
                .parameters(a.getParameters())
                .description(a.getDescription())
                .build()).collect(Collectors.toList());
    }

    private List<Action> toActionEntities(List<PolicyRequest.ActionRequest> actions) {
        if (actions == null) return new ArrayList<>();
        return actions.stream().map(a -> Action.builder()
                .type(parseEnum(ActionType.class, a.getType()))
                .parameters(a.getParameters())
                .description(a.getDescription())
                .build()).collect(Collectors.toList());
    }

    // ==================== Utility ====================

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s value: '%s'. Valid values: %s",
                            enumClass.getSimpleName(), value, java.util.Arrays.toString(enumClass.getEnumConstants())));
        }
    }
}
