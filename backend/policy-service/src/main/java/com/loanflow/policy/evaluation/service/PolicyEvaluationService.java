package com.loanflow.policy.evaluation.service;

import com.loanflow.policy.evaluation.dto.PolicyEvaluationRequest;
import com.loanflow.policy.evaluation.dto.PolicyEvaluationResponse;

/**
 * Service interface for policy evaluation.
 * Evaluates loan applications against active policies to determine eligibility,
 * pricing, and required actions.
 */
public interface PolicyEvaluationService {

    /**
     * Evaluate a loan application against all active policies for the given loan type.
     *
     * @param request the evaluation request containing application data
     * @return evaluation response with decision, matched policies, and triggered actions
     */
    PolicyEvaluationResponse evaluate(PolicyEvaluationRequest request);
}
