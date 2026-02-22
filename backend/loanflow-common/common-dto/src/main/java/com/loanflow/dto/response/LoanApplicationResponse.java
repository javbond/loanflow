package com.loanflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Loan application response")
public class LoanApplicationResponse {

    @Schema(description = "Unique loan application ID")
    private UUID id;

    @Schema(description = "Human-readable application number", example = "LN-2024-001234")
    private String applicationNumber;

    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Type of loan")
    private String loanType;

    @Schema(description = "Requested loan amount")
    private BigDecimal requestedAmount;

    @Schema(description = "Approved loan amount")
    private BigDecimal approvedAmount;

    @Schema(description = "Interest rate (per annum)")
    private BigDecimal interestRate;

    @Schema(description = "Loan tenure in months")
    private Integer tenureMonths;

    @Schema(description = "Monthly EMI amount")
    private BigDecimal emiAmount;

    @Schema(description = "Current application status")
    private String status;

    @Schema(description = "Current workflow stage")
    private String workflowStage;

    @Schema(description = "Purpose of the loan")
    private String purpose;

    @Schema(description = "Branch code")
    private String branchCode;

    @Schema(description = "Assigned loan officer")
    private String assignedOfficer;

    @Schema(description = "CIBIL score")
    private Integer cibilScore;

    @Schema(description = "Risk category")
    private String riskCategory;

    @Schema(description = "Processing fee")
    private BigDecimal processingFee;

    @Schema(description = "Credit bureau data source", example = "REAL")
    private String bureauDataSource;

    @Schema(description = "Credit bureau pull timestamp")
    private Instant bureauPullTimestamp;

    @Schema(description = "Whether income was verified")
    private Boolean incomeVerified;

    @Schema(description = "Verified monthly income")
    private BigDecimal verifiedMonthlyIncome;

    @Schema(description = "Debt-to-Income ratio")
    private BigDecimal dtiRatio;

    @Schema(description = "Income data source", example = "REAL")
    private String incomeDataSource;

    @Schema(description = "Expected disbursement date")
    private LocalDate expectedDisbursementDate;

    @Schema(description = "Application submission date")
    private Instant submittedAt;

    @Schema(description = "Application creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "List of documents attached")
    private List<DocumentInfo> documents;

    @Schema(description = "Workflow history")
    private List<WorkflowHistory> workflowHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Document information")
    public static class DocumentInfo {
        private UUID documentId;
        private String documentType;
        private String fileName;
        private String status;
        private Instant uploadedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Workflow stage history")
    public static class WorkflowHistory {
        private String fromStage;
        private String toStage;
        private String action;
        private String performedBy;
        private String comments;
        private Instant timestamp;
    }
}
