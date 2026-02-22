package com.loanflow.loan.creditbureau.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Summary of a single credit account from CIBIL report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary implements Serializable {
    private String accountType;       // Credit Card, Personal Loan, Home Loan, etc.
    private String lenderName;
    private double currentBalance;
    private double amountOverdue;
    private String dpdStatus;         // "000", "030", "060", "090+"
    private String accountStatus;     // Active, Closed, Written Off
    private LocalDate openDate;
    private LocalDate lastPaymentDate;
}
