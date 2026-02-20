package com.loanflow.loan.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO to complete a workflow task with a decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {

    @NotBlank(message = "Decision is required")
    @Pattern(regexp = "APPROVED|REJECTED|REFERRED", message = "Decision must be APPROVED, REJECTED, or REFERRED")
    private String decision;

    private String comments;

    private BigDecimal approvedAmount;

    private BigDecimal interestRate;

    private String rejectionReason;
}
