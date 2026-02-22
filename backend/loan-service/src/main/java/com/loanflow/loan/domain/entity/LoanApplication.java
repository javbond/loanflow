package com.loanflow.loan.domain.entity;

import com.loanflow.loan.domain.enums.LoanStatus;
import com.loanflow.loan.domain.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "loan_applications", schema = "application",
        indexes = {
                @Index(name = "idx_loan_app_customer", columnList = "customer_id"),
                @Index(name = "idx_loan_app_customer_email", columnList = "customer_email"),
                @Index(name = "idx_loan_app_status", columnList = "status"),
                @Index(name = "idx_loan_app_number", columnList = "application_number", unique = true),
                @Index(name = "idx_loan_app_branch", columnList = "branch_code")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {

    private static final BigDecimal MINIMUM_LOAN_AMOUNT = new BigDecimal("10000");
    private static final BigDecimal MAXIMUM_LOAN_AMOUNT = new BigDecimal("100000000");
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_number", unique = true, length = 20)
    private String applicationNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 30)
    private LoanType loanType;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_amount", precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LoanStatus status = LoanStatus.DRAFT;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "assigned_officer")
    private UUID assignedOfficer;

    @Column(name = "cibil_score")
    private Integer cibilScore;

    @Column(name = "risk_category", length = 20)
    private String riskCategory;

    @Column(name = "processing_fee", precision = 15, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "bureau_data_source", length = 20)
    private String bureauDataSource;

    @Column(name = "bureau_pull_timestamp")
    private Instant bureauPullTimestamp;

    @Column(name = "workflow_instance_id", length = 50)
    private String workflowInstanceId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Column(name = "expected_disbursement_date")
    private LocalDate expectedDisbursementDate;

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

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    // ==================== Business Methods ====================

    /**
     * Generate unique application number
     * Format: LN-YYYY-NNNNNN
     */
    public void generateApplicationNumber() {
        if (this.applicationNumber == null) {
            String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            long seq = counter.incrementAndGet() % 1000000;
            this.applicationNumber = String.format("LN-%s-%06d", year, seq);
        }
    }

    /**
     * Validate loan application
     */
    public void validate() {
        if (requestedAmount.compareTo(MINIMUM_LOAN_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                    String.format("Minimum loan amount is INR %s", MINIMUM_LOAN_AMOUNT));
        }
        if (requestedAmount.compareTo(MAXIMUM_LOAN_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    String.format("Maximum loan amount is INR %s", MAXIMUM_LOAN_AMOUNT));
        }
        if (tenureMonths > loanType.getMaxTenureMonths()) {
            throw new IllegalArgumentException(
                    String.format("Maximum tenure for %s is %d months",
                            loanType.getDisplayName(), loanType.getMaxTenureMonths()));
        }
        if (tenureMonths < 6) {
            throw new IllegalArgumentException("Minimum tenure is 6 months");
        }
    }

    /**
     * Submit application for processing
     */
    public void submit() {
        if (status != LoanStatus.DRAFT && status != LoanStatus.RETURNED) {
            throw new IllegalStateException(
                    String.format("Cannot submit application in %s status", status));
        }
        this.status = LoanStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    /**
     * Transition to a new status with validation
     */
    public void transitionTo(LoanStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", status, newStatus));
        }
        this.status = newStatus;
    }

    /**
     * Approve the loan application
     */
    public void approve(BigDecimal approvedAmount, BigDecimal interestRate) {
        if (status != LoanStatus.UNDERWRITING && status != LoanStatus.CONDITIONALLY_APPROVED
                && status != LoanStatus.REFERRED) {
            throw new IllegalStateException("Can only approve from UNDERWRITING, CONDITIONALLY_APPROVED, or REFERRED status");
        }
        if (approvedAmount.compareTo(requestedAmount) > 0) {
            throw new IllegalArgumentException("Approved amount cannot exceed requested amount");
        }

        this.approvedAmount = approvedAmount;
        this.interestRate = interestRate;
        this.emiAmount = calculateEmi(approvedAmount, interestRate, tenureMonths);
        this.status = LoanStatus.APPROVED;
        this.approvedAt = Instant.now();

        // Calculate expected disbursement (7 business days from approval)
        this.expectedDisbursementDate = LocalDate.now().plusDays(10);
    }

    /**
     * Reject the loan application
     */
    public void reject(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot reject an application in terminal status");
        }
        this.rejectionReason = reason;
        this.status = LoanStatus.REJECTED;
        this.rejectedAt = Instant.now();
    }

    /**
     * Cancel the application
     */
    public void cancel(String reason) {
        if (status == LoanStatus.DISBURSED || status == LoanStatus.CLOSED) {
            throw new IllegalStateException("Cannot cancel disbursed or closed application");
        }
        this.rejectionReason = reason;
        this.status = LoanStatus.CANCELLED;
    }

    /**
     * Calculate EMI using standard formula
     * EMI = P * R * (1+R)^N / ((1+R)^N - 1)
     * where P = Principal, R = Monthly interest rate, N = Number of months
     */
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal.compareTo(BigDecimal.ZERO) == 0 || tenureMonths == 0) {
            return BigDecimal.ZERO;
        }

        MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

        // Monthly interest rate = Annual rate / 12 / 100
        BigDecimal monthlyRate = annualRate
                .divide(new BigDecimal("1200"), mc);

        // (1 + R)^N
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(tenureMonths, mc);

        // P * R * (1+R)^N
        BigDecimal numerator = principal.multiply(monthlyRate, mc).multiply(power, mc);

        // (1+R)^N - 1
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        // EMI = numerator / denominator
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Check if application is in active processing
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Check if application can be edited
     */
    public boolean isEditable() {
        return status == LoanStatus.DRAFT || status == LoanStatus.RETURNED;
    }
}
