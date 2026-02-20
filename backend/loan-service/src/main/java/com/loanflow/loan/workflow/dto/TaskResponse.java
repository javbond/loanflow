package com.loanflow.loan.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a Flowable workflow task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResponse {

    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String assignee;
    private List<String> candidateGroups;

    // Loan application context
    private String applicationId;
    private String applicationNumber;
    private String loanType;
    private String customerEmail;
    private String requestedAmount;
    private Integer cibilScore;
    private String riskCategory;

    // Workflow metadata
    private String processInstanceId;
    private Instant createdAt;
    private Instant dueDate;
    private String formKey;
}
